package org.gradle.buildeng.analysis.consumer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.pubsub.v1.ProjectSubscriptionName
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
import rx.exceptions.Exceptions
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicReference


class BuildConsumer(private val geServer: ServerConnectionInfo) {
    private val httpClient = HttpClient.newClient(geServer.socketAddress).unsafeSecure()
    private val objectMapper = ObjectMapper()
//    private val eventTypes = setOf("DaemonState", "BuildStarted", "BuildRequestedTasks", "BuildModes", "Hardware", "Os", "Jvm", "JvmArgs", "Locality", "Encoding", "FileRefRoots", "ProjectStructure", "ScopeIds", "LoadBuildStarted", "LoadBuildFinished", "ProjectEvaluationStarted", "ProjectEvaluationFinished", "NetworkDownloadActivityStarted", "NetworkDownloadActivityFinished", "TaskStarted", "TaskFinished", "TestStarted", "TestFinished", "BuildAgent", "ExceptionData", "BuildFinished", "BasicMemoryStats", "UserTag", "UserLink", "UserNamedValue", "OutputLogEvent", "OutputStyledTextEvent")
    val storage: Storage = StorageOptions.getDefaultInstance().service

    object MessageReceiverImpl : MessageReceiver {
        val messages: BlockingQueue<PubsubMessage> = LinkedBlockingDeque()

        override fun receiveMessage(message: PubsubMessage?, consumer: AckReplyConsumer?) {
            messages.offer(message)
            consumer?.ack()
        }
    }

    fun consume(subscriptionId: String, gcsBucketName: String) {
        // TODO: Create subscription dynamically
        val subscriptionName = ProjectSubscriptionName.of(ServiceOptions.getDefaultProjectId(), subscriptionId)
        var subscriber: Subscriber? = null

        try {
            subscriber = Subscriber.newBuilder(subscriptionName, MessageReceiverImpl).build()
            subscriber.startAsync().awaitRunning()

            // TODO: configure parallelism
            while (true) {
                val message = MessageReceiverImpl.messages.take()
                println("Got message: ${message.messageId}")
                val buildId = parse(message.data.toStringUtf8())["buildId"].asText()

                buildEventStream(buildId)
                        .doOnSubscribe({ println("Streaming events for build $buildId") })
                        .map({ serverSentEvent ->
                            val event = serverSentEvent.contentAsString()
                            assert(serverSentEvent.release())
                            event
                        })
                        .toList()
                        .map({ events ->
                            val blobKey = "$buildId-build-events-json.txt"
                            val blobInfo = BlobInfo
                                    .newBuilder(BlobId.of(gcsBucketName, blobKey))
                                    .setContentType("text/plain")
                                    .build()
                            storage.create(blobInfo, events.joinToString("\n").toByteArray())
                            println("Creating blob $blobKey")
                        })
                        .toBlocking()
                        .subscribe({
                            // Awaiteth thou writes to Storage
                            println("done")
                        })
            }
        } finally {
            subscriber?.stopAsync()
        }
    }

    private fun buildEventStream(buildId: String): Observable<ServerSentEvent> {
        return resume("/build-export/v1/build/$buildId/events", null)
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
                        println("Error streaming $url: ${it.message}, resuming from null...")
                        resume(url, null)
                    }
                })
    }

    private fun getContentAsSse(response: HttpClientResponse<ByteBuf>): ContentSource<ServerSentEvent> {
        if (response.status != HttpResponseStatus.OK) {
            return ContentSource(IllegalArgumentException("HTTP Status: ${response.status}"))
        }

        return response.contentAsServerSentEvents
    }

    private fun parse(input: String): JsonNode {
        try {
            return objectMapper.readTree(input)
        } catch (e: IOException) {
            throw Exceptions.propagate(e)
        }
    }
}
