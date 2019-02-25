package org.gradle.buildeng.analysis.producer

import com.google.api.core.ApiFutures
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil
import io.reactivex.netty.channel.ContentSource
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import io.reactivex.netty.protocol.http.sse.ServerSentEvent
import org.gradle.buildeng.analysis.common.ServerConnectionInfo
import rx.Observable
import java.nio.charset.Charset
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference


class BuildProducer(private val geServer: ServerConnectionInfo) {
    private val httpClient = HttpClient.newClient(geServer.socketAddress).unsafeSecure()

    fun produceFrom(pubSubTopicId: String, since: Instant, lastEventId: String?) {
        val topicName = ProjectTopicName.of(ServiceOptions.getDefaultProjectId(), pubSubTopicId)
        val publisher = Publisher.newBuilder(topicName).build()

        buildStream(since, lastEventId)
                .doOnSubscribe({ println("Streaming builds...") })
                .doAfterTerminate({
                    println("Shutting down publisher...")
                    publisher?.shutdown()
                })
                .map({ serverSentEvent ->
                    println("processing ${serverSentEvent.eventId.toString(Charset.defaultCharset())}")
                    val message = PubsubMessage.newBuilder()
                            .setData(ByteString.copyFromUtf8(serverSentEvent.contentAsString()))
                            .build()

                    assert(serverSentEvent.release())

                    // NOTE: Messages are automatically batched by the PubSub API
                    publisher.publish(message)
                })
                .toList()
                .toBlocking()
                .subscribe({ futures ->
                    // Wait on any pending requests
                    val messageIds = ApiFutures.allAsList(futures).get()
                    println("${messageIds.size} messages published")
                })
        return
    }

    private fun getContentAsSse(response: HttpClientResponse<ByteBuf>): ContentSource<ServerSentEvent> {
        if (response.status != HttpResponseStatus.OK) {
            return ContentSource(IllegalArgumentException("HTTP Status: ${response.status}"))
        }

        return response.contentAsServerSentEvents
    }

    private fun buildStream(since: Instant, lastEventId: String?): Observable<ServerSentEvent> {
        return resume("/build-export/v1/builds/since/${since.toEpochMilli()}", lastEventId)
    }

    private fun resume(url: String, lastEventId: String?): Observable<ServerSentEvent> {
        val eventId = AtomicReference<String>()

        val authByteBuf = Unpooled.copiedBuffer("${geServer.username}:${geServer.password}".toCharArray(), CharsetUtil.UTF_8)
        val authValue = "Basic " + Base64.encode(authByteBuf).toString(CharsetUtil.UTF_8)
        var request = httpClient
                .createGet(url)
                .addHeader("Authorization", authValue)
                .addHeader("Accept", "text/event-stream")

        if (lastEventId != null) {
            request = request.addHeader("Last-Event-ID", lastEventId)
        }

        return request
                .flatMap({ response -> getContentAsSse(response) })
                .doOnNext({ serverSentEvent: ServerSentEvent -> eventId.set(serverSentEvent.eventIdAsString) })
                .onErrorResumeNext({
                    println("Error: ${it.message} — ${it.cause}")

                    if (eventId.get() != null) {
                        println("Error streaming ${eventId.get()} from $url: ${it.message}, resuming from " + eventId.get() + "...")
                        resume(url, eventId.get())
                    } else {
                        println("Error streaming $url: ${it.message}, resuming from null ...")
                        resume(url, null)
                    }
                })
    }
}
