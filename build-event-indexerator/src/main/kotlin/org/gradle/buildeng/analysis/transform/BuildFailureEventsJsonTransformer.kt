package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.BuildEvent
import org.gradle.buildeng.analysis.model.BuildFailure
import org.gradle.buildeng.analysis.model.Failure
import java.time.Instant

class BuildFailureEventsJsonTransformer : EventsJsonTransformer() {

    fun transform(fileContents: String): String {
        val list = fileContents.split("\n")
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        // Read first line, then everything else is events
        val header = objectReader.readTree(list.first())

        var rootProjectName = "UNKNOWN_ROOT_PROJECT"
        var buildAgentId = "UNKNOWN_BUILD_AGENT"
        val buildId = header.get("buildId").asText()
        val timestamp = Instant.ofEpochMilli(header.get("timestamp").asLong())
        val exceptions = mutableListOf<Failure>()

        val rawBuildEvents = list.drop(1)

        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            when (buildEvent?.type?.eventType) {
                "BuildAgent" -> buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> {
                    // This event is triggered for every included build, so is the only way to get root project
                    if (buildEvent.data.path("projects").any { project -> project.path("buildPath").asText() == ":" }) {
                        rootProjectName = buildEvent.data.get("rootProjectName").asText()
                    }
                }
                "ExceptionData" -> {
                    val exceptionEvent = BuildEvent.fromJson(objectReader.readTree(it))!!

                    exceptionEvent.data.get("exceptions").fields().asSequence().forEach { exceptionKVs ->
                        exceptions.add(Failure(
                                exceptionKVs.key,
                                exceptionKVs.value.path("taskId").asText(),
                                exceptionKVs.value.path("className").asText(),
                                exceptionKVs.value.path("message").asText(),
                                exceptionKVs.value.path("causes").map { c -> c.asText() }
                        ))
                    }
                }
            }
        }

        val buildFailure = BuildFailure(buildId, rootProjectName, buildAgentId, timestamp, exceptions)
        return objectWriter.writeValueAsString(objectMapper.convertValue(buildFailure, JsonNode::class.java))
    }
}

