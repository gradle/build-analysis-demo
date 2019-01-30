package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.gradle.buildeng.analysis.common.DurationSerializer
import org.gradle.buildeng.analysis.common.InstantSerializer
import org.gradle.buildeng.analysis.common.NullAvoidingStringSerializer
import org.gradle.buildeng.analysis.model.BuildCacheInteraction
import org.gradle.buildeng.analysis.model.BuildEvent
import java.time.Duration

/**
 * Transforms BuildEvent JSON into {@see BuildCacheInteraction}s.
 */
class BuildCacheEventsJsonTransformer {

    private val objectMapper = ObjectMapper()
    private val objectReader = objectMapper.reader()
    private val objectWriter = objectMapper.writer()

    init {
        objectMapper.registerModule(object : SimpleModule() {
            init {
                addSerializer(InstantSerializer())
                addSerializer(DurationSerializer())
                addSerializer(NullAvoidingStringSerializer())
            }
        })
    }

    fun transform(fileContents: String): List<String> {
        val buildCacheInteractions = mutableMapOf<String, BuildCacheInteraction>()

        // Drop header line
        val list = fileContents.split("\n").drop(1)
        list.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            // Ignoring different build event versions here because every version has what we want
            when (buildEvent?.type?.eventType) {
                "BuildCachePackStarted", "BuildCacheUnpackStarted", "BuildCacheRemoteLoadStarted", "BuildCacheRemoteStoreStarted" -> {
                    val id = buildEvent.data.get("id").asText()
                    val taskId = buildEvent.data.path("task").asText()
                    val cacheKey = buildEvent.data.path("cacheKey").asText()
                    val type = buildEvent.type.eventType.removeSuffix("Started")
                    buildCacheInteractions[id] = BuildCacheInteraction(id, taskId, type, cacheKey, buildEvent.timestamp)
                }
                "BuildCachePackFinished", "BuildCacheUnpackFinished", "BuildCacheRemoteLoadFinished", "BuildCacheRemoteStoreFinished" -> {
                    val id = buildEvent.data.get("id").asText()
                    val startedInteraction = buildCacheInteractions[id]!!
                    buildCacheInteractions[id] = startedInteraction.copy(
                            duration = Duration.between(startedInteraction.startTimestamp, buildEvent.timestamp),
                            failureId = buildEvent.data.path("failureId").asText()
                    )
                }
            }
        }

        return buildCacheInteractions.map {
            objectWriter.writeValueAsString(objectMapper.convertValue(it.value, JsonNode::class.java))
        }
    }
}
