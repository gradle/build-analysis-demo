package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.BuildCacheInteraction
import org.gradle.buildeng.analysis.model.BuildEvent
import java.time.Duration

/**
 * Transforms BuildEvent JSON into {@see BuildCacheInteraction}s.
 */
class BuildCacheEventsJsonTransformer : EventsJsonTransformer() {

    override fun transform(list: List<String>): String {
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        val buildCacheInteractions = mutableMapOf<String, BuildCacheInteraction>()

        list.drop(1).filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            // Ignoring different build event versions here because every version has what we want
            when (buildEvent?.type?.eventType) {
                "BuildCachePackStarted", "BuildCacheUnpackStarted", "BuildCacheRemoteLoadStarted", "BuildCacheRemoteStoreStarted" -> {
                    val id = buildEvent.data.get("id").asText()
                    val taskId = buildEvent.data.path("task").asText()
                    val cacheKey = buildEvent.data.path("cacheKey").asText()
                    buildCacheInteractions[id] = BuildCacheInteraction(id, buildEvent.type.eventType, taskId, cacheKey, buildEvent.timestamp)
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

        val outputJson = objectMapper.convertValue(buildCacheInteractions, JsonNode::class.java)
        return objectWriter.writeValueAsString(outputJson)
    }
}
