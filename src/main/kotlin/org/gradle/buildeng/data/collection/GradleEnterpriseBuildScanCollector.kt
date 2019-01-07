package org.gradle.buildeng.data.collection

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil
import io.reactivex.netty.channel.ContentSource
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import io.reactivex.netty.protocol.http.sse.ServerSentEvent
import rx.Observable
import rx.exceptions.Exceptions
import java.io.IOException
import java.io.Writer
import java.net.InetSocketAddress
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine


class GradleEnterpriseBuildScanCollector(private val serverUrl: String, private val username: String, private val password: String, private val writer: Writer) {
    private val gradleEnterpriseServer = InetSocketAddress(serverUrl, 443)
    private val httpClient = HttpClient.newClient(gradleEnterpriseServer).unsafeSecure()
    // FIXME: private val httpClient = HttpClient.newClient(gradleEnterpriseServer).secure(defaultSSLEngineForClient())
    // Error streaming /build-export/v1/build/ai3ssga4slwi2/events?eventTypes=BuildAgent: SSLEngine closed already, resuming from null ...
    private val throttleMs = 30
    private val objectMapper = ObjectMapper()
    private val buildData = mutableListOf<String>()

    fun writeBuilds() {
        val since1Day = Instant.now().minus(Duration.ofHours(1))

        buildStream(since1Day)
                .doOnSubscribe({ println("Streaming builds...") })
                .map({ buildSse ->
                    parse(buildSse)
                })
                // TODO: add sse to buildData
                .map({ buildJson -> buildJson["buildId"].asText() })
                .flatMap({ buildId ->
                    buildEventStream(buildId, setOf("BuildAgent", "BuildStarted", "BuildFinished", "ProjectEvaluationStarted", "ProjectEvaluationFinished"))
                            .doOnSubscribe({ buildData.add(buildId) })
                            .filter({ sse -> sse.eventTypeAsString == "BuildEvent" })
                            .map({ buildEventSse -> parse(buildEventSse) }) // TODO: write to file according to buildId

                }, throttleMs)
                .toList()
                .toBlocking()
                .subscribe({ println(it) })

        try {
            println("Writing ${buildData.size} entries to output file")
            buildData.forEach {
                writer.append("$it\n")
            }
        } catch (e: IOException) {
            println(e.printStackTrace())
        } finally {
            writer.close()
        }

        return
    }

    private fun buildStream(since: Instant): Observable<ServerSentEvent> {
        return resume("/build-export/v1/builds/since/${since.toEpochMilli()}", null)
    }


    private fun buildEventStream(buildId: String, eventTypes: Set<String>): Observable<ServerSentEvent> {
        return resume("/build-export/v1/build/$buildId/events?eventTypes=${eventTypes.joinToString(",")}", null)
    }

    private fun resume(url: String, lastEventId: String?): Observable<ServerSentEvent> {
        val eventId = AtomicReference<String>()

        val authByteBuf = Unpooled.copiedBuffer("$username:$password".toCharArray(), CharsetUtil.UTF_8)
        val authValue = "Basic " + Base64.encode(authByteBuf).toString(CharsetUtil.UTF_8)
        var request = httpClient
                .createGet(url)
                .addHeader("Authorization", authValue)
                .addHeader("Accept", "text/event-stream")

        if (lastEventId != null) {
            request = request.addHeader("Last-Event-ID", lastEventId)
        }

        return request
                .flatMap({response -> getContentAsSse(response)})
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

    private fun getContentAsSse(response: HttpClientResponse<ByteBuf>): ContentSource<ServerSentEvent> {
        if (response.status != HttpResponseStatus.OK) {
            return ContentSource(IllegalArgumentException("HTTP Status: ${response.status}"))
        }

        return response.contentAsServerSentEvents
    }

    private fun parse(serverSentEvent: ServerSentEvent): JsonNode {
        try {
            return objectMapper.readTree(serverSentEvent.contentAsString())
        } catch (e: IOException) {
            throw Exceptions.propagate(e)
        } finally {
            val deallocated = serverSentEvent.release()
            assert(deallocated)
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun defaultSSLEngineForClient(): SSLEngine {
        val sslCtx = SSLContext.getDefault()
        val sslEngine = sslCtx.createSSLEngine(serverUrl, 443)
        sslEngine.useClientMode = true
        return sslEngine
    }
}
