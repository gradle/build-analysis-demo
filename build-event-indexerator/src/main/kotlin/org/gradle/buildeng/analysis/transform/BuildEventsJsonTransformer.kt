package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.*
import org.gradle.buildeng.analysis.model.BuildEvent
import java.time.Duration
import java.time.Instant

class BuildEventsJsonTransformer : EventsJsonTransformer() {
    fun transform(input: String): String {
        return transform(input.split("\n"))
    }

    fun transform(list: List<String>): String {
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        // Read first line, then everything else is events
        val header = objectReader.readTree(list.first())

        var buildAgentId = "UNKNOWN_BUILD_AGENT_ID"
        var rootProjectName = "UNKNOWN_ROOT_PROJECT"
        val buildId = header.get("buildId").asText()
        val buildToolVersion = header.get("gradleVersion").asText()
        var buildStartTimestamp = Instant.ofEpochMilli(header.get("timestamp").asLong())
        var wallClockDuration: Duration? = null
        var failureId = ""
        var failed = false

        val rawBuildEvents = list.drop(1)
        val buildRequestedTasks = mutableListOf<String>()
        val buildExcludedTasks = mutableListOf<String>()
        val environmentParameters = mutableListOf<EnvironmentParameter>()
        val userLinks = mutableListOf<UserLink>()
        val userNamedValues = mutableListOf<UserNamedValue>()
        val userTags = mutableListOf<String>()

        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            when (buildEvent?.type?.eventType) {
                // NOTE: header (build) timestamp represents when build scan was received, so make sure we use buildStart timestamp to measure build duration
                "BuildStarted" -> buildStartTimestamp = buildEvent.timestamp
                "BuildFinished" -> {
                    wallClockDuration = Duration.between(buildStartTimestamp!!, buildEvent.timestamp)
                    failureId = buildEvent.data.path("failureId").asText()
                    failed = !buildEvent.data.path("failureId").isNull
                }
                "BuildAgent" -> buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> {
                    // This event is triggered for every included build, so is the only way to get root project
                    if (buildEvent.data.path("projects").any { project -> project.path("buildPath").asText() == ":" }) {
                        rootProjectName = buildEvent.data.get("rootProjectName").asText()
                    }
                }
                "BuildRequestedTasks" -> {
                    buildRequestedTasks.addAll(buildEvent.data.get("requested").map { arg -> arg.asText() })
                    buildExcludedTasks.addAll(buildEvent.data.get("excluded").map { arg -> arg.asText() })
                }
                "BuildModes", "DaemonState", "Hardware", "Os", "Jvm", "JvmArgs" -> {
                    val environmentData = objectWriter.writeValueAsString(buildEvent.data)
                    environmentParameters.add(EnvironmentParameter(buildEvent.type.eventType, environmentData))
                }
                "UserLink" -> userLinks.add(UserLink(buildEvent.data.get("label").asText(), buildEvent.data.get("url").asText()))
                "UserNamedValue" -> userNamedValues.add(UserNamedValue(buildEvent.data.get("key").asText(), buildEvent.data.get("value").asText()))
                "UserTag" -> userTags.add(buildEvent.data.get("tag").asText())
            }
        }

        val build = Build(buildId, rootProjectName, "Gradle", buildToolVersion, buildAgentId, buildRequestedTasks, buildExcludedTasks, environmentParameters, buildStartTimestamp!!, wallClockDuration!!, failureId, failed, userLinks, userNamedValues, userTags)
        return objectWriter.writeValueAsString(objectMapper.convertValue(build, JsonNode::class.java))
    }
}
