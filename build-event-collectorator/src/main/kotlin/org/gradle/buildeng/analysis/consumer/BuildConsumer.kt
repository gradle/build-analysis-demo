package org.gradle.buildeng.analysis.consumer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference


class BuildConsumer(private val geServer: ServerConnectionInfo) {
    private val httpClient = HttpClient.newClient(geServer.socketAddress).unsafeSecure()
    private val objectMapper = ObjectMapper()
    private val eventTypes = setOf("BuildStarted", "BuildRequestedTasks", "BuildModes", "DaemonState", "Hardware", "Os", "Jvm", "JvmArgs", "ProjectStructure", "TaskStarted", "TaskFinished", "BuildCacheRemoteLoadStarted", "BuildCacheRemoteStoreStarted", "BuildCachePackStarted", "BuildCachePackFinished", "BuildCacheUnpackStarted", "BuildCacheUnpackFinished", "BuildCacheRemoteLoadFinished", "TestStarted", "TestFinished", "BuildAgent", "ExceptionData", "BuildFinished", "UserTag", "UserLink", "UserNamedValue") // "LoadBuildStarted", "LoadBuildFinished", "OutputLogEvent", "OutputStyledTextEvent", "NetworkDownloadActivityStarted", "NetworkDownloadActivityFinished", "BasicMemoryStats", "ProjectEvaluationStarted", "ProjectEvaluationFinished", "Locality", "Encoding", "ScopeIds", "FileRefRoots",
    private val storage: Storage = StorageOptions.getDefaultInstance().service

    fun consume(since: Instant, lastEventId: String?, gcsBucketName: String) {
        buildStream(since, lastEventId)
                .doOnSubscribe({ println("Streaming builds from [${geServer.socketAddress.hostName}] to gs://$gcsBucketName/") })
                .map({ serverSentEvent -> parse(serverSentEvent) })
                .map({ json -> json["buildId"].asText() })
                .flatMap({ buildId ->
                    buildEventStream(buildId)
                            .doOnSubscribe({ println("Streaming events for build $buildId ") })
                            .toList()
                            .map({ serverSentEvents ->
                                val blobKey = "$buildId-build-events-json.txt"
                                val blobInfo = BlobInfo
                                        .newBuilder(BlobId.of(gcsBucketName, blobKey))
                                        .setContentType("text/plain")
                                        .build()

                                storage.create(blobInfo, byteBufsToJoinedArray(serverSentEvents))

                                serverSentEvents.forEach { assert(it.release()) }
                                println("[${System.currentTimeMillis()}] $blobKey written")
                            })
                }, 32)
                .toBlocking()
                .subscribe()
    }

    fun byteBufsToJoinedArray(input: List<ByteBufHolder>): ByteArray {
        val outputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        val newlineBytes = "\n".toByteArray()

        input.forEach { serverSentEvent ->
            val event = serverSentEvent.content()
            if (event.hasArray()) {
                outputStream.write(event.array())
            } else {
                val bytes = ByteArray(event.readableBytes())
                event.readBytes(bytes)
                outputStream.write(bytes)
            }
            outputStream.write(newlineBytes)
        }

        return outputStream.toByteArray()
    }

    fun parse(serverSentEvent: ServerSentEvent): JsonNode {
        try {
            return objectMapper.readTree(serverSentEvent.contentAsString())
        } catch (e: IOException) {
            throw Exceptions.propagate(e)
        } finally {
            val deallocated = serverSentEvent.release()
            assert(deallocated)
        }
    }

    private fun buildStream(since: Instant, lastEventId: String?): Observable<ServerSentEvent> {
        return resume("/build-export/v1/builds/since/${since.toEpochMilli()}", lastEventId)
    }

    private fun buildEventStream(buildId: String): Observable<ServerSentEvent> {
        return resume("/build-export/v1/build/$buildId/events?eventTypes=${eventTypes.joinToString(",")}", null)
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
                .flatMap({ response -> getContentAsSse(response) }, 4)
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
}
