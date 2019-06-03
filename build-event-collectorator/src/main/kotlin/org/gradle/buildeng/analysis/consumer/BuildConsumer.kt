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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference


class BuildConsumer(private val geServer: ServerConnectionInfo) {
    private val httpClient = HttpClient.newClient(geServer.socketAddress).unsafeSecure()
    private val objectMapper = ObjectMapper()
    private val eventTypes = setOf(
            "BuildStarted", "MvnBuildStarted", "BuildFinished", "MvnBuildFinished", "MvnExecutionStarted", "MvnExecutionFinished", "MvnProjectStarted", "MvnProjectFinished",
            "LoadBuildStarted", "LoadBuildFinished", "MvnSettingsStarted", "MvnSettingsFinished",
            "ProjectEvaluationStarted", "ProjectEvaluationFinished", "MvnToolchainsStarted", "MvnToolchainsFinished",
            "PluginApplicationStarted", "MvnPluginApplication",
            "BuildRequestedTasks", "MvnBuildRequestedGoals",
            "BuildModes", "DaemonState", "JvmArgs", // Gradle-only
            "Hardware", "MvnHardware", "Os", "MvnOs", "Jvm", "MvnJvm",
            "ProjectStructure", "MvnProjectStructure", "BasicMemoryStats", "MvnBasicMemoryStats", "Locality", "MvnLocality", "Encoding", "MvnEncoding", "ScopeIds", "MvnScopeIds", "FileRefRoots", "MvnFileRefRoots",
            "TaskStarted", "TaskFinished", "MvnGoalExecutionStarted", "MvnGoalExecutionFinished",
            "BuildCacheRemoteLoadStarted", "BuildCacheRemoteStoreStarted", "BuildCacheRemoteStoreFinished", "BuildCachePackStarted", "BuildCachePackFinished", "BuildCacheUnpackStarted", "BuildCacheUnpackFinished", "BuildCacheRemoteLoadFinished",
            "MvnBuildCacheRemoteLoadStarted", "MvnBuildCacheRemoteStoreStarted", "MvnBuildCacheRemoteStoreFinished", "MvnBuildCachePackStarted", "MvnBuildCachePackFinished", "MvnBuildCacheUnpackStarted", "MvnBuildCacheUnpackFinished", "MvnBuildCacheRemoteLoadFinished",
            "TestStarted", "TestFinished", // Tests Gradle-only for now
            "BuildAgent", "MvnBuildAgent",
            "ExceptionData", "MvnExceptionData",
            "UserTag", "MvnUserTag", "UserLink", "MvnUserLink", "UserNamedValue", "MvnUserNamedValue",
            "Repository", "ConfigurationResolutionData", "NetworkDownloadActivityStarted", "NetworkDownloadActivityFinished" // Network/repo Gradle-only
    ) // "OutputLogEvent", "OutputStyledTextEvent"
    private val storage: Storage = StorageOptions.getDefaultInstance().service
    private val daySlashyFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    fun consume(startEpoch: Long, gcsBucketName: String) {
        buildStream(startEpoch)
                .doOnSubscribe { println("Streaming builds from [${geServer.socketAddress.hostName}] to gs://$gcsBucketName/") }
                .map { serverSentEvent -> parse(serverSentEvent) }
                .map { json -> Pair(json["buildId"].asText(), json["timestamp"].asLong()) }
                .flatMap({ (buildId, timestamp) ->
                    buildEventStream(buildId)
                            .doOnSubscribe { println("Streaming events for build $buildId at $timestamp") }
                            .toList()
                            .map { serverSentEvents ->
                                try {
                                    val localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate()
                                    val blobKey = "${localDate.format(daySlashyFormat)}/$buildId-json.txt"
                                    val blobInfo = BlobInfo
                                            .newBuilder(BlobId.of(gcsBucketName, blobKey))
                                            .setContentType("text/plain")
                                            .build()

                                    storage.create(blobInfo, byteBufsToJoinedArray(serverSentEvents))
                                    println("[${System.currentTimeMillis()}] $blobKey written")
                                } finally {
                                    serverSentEvents.forEach { assert(it.release()) }
                                }
                            }
                }, 20)
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

    private fun buildStream(fromTimestamp: Long): Observable<ServerSentEvent> {
        return resume("/build-export/v1/builds/since/$fromTimestamp")
    }

    private fun buildEventStream(buildId: String): Observable<ServerSentEvent> {
        return resume("/build-export/v1/build/$buildId/events?eventTypes=${eventTypes.joinToString(",")}", null)
    }

    private fun resume(url: String, lastEventId: String? = null): Observable<ServerSentEvent> {
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
                .flatMap { response -> getContentAsSse(response) }
                .doOnNext { serverSentEvent: ServerSentEvent -> eventId.set(serverSentEvent.eventIdAsString) }
                .onErrorResumeNext {
                    println("Error: ${it.message} — ${it.cause}")

                    if (eventId.get() != null) {
                        println("Error streaming ${eventId.get()} from $url: ${it.message}, resuming from " + eventId.get() + "...")
                        resume(url, eventId.get())
                    } else {
                        println("Error streaming $url: ${it.message}, resuming from null...")
                        resume(url, null)
                    }
                }
    }

    private fun getContentAsSse(response: HttpClientResponse<ByteBuf>): ContentSource<ServerSentEvent> {
        if (response.status != HttpResponseStatus.OK) {
            return ContentSource(IllegalArgumentException("HTTP Status: ${response.status}"))
        }

        return response.contentAsServerSentEvents
    }
}
