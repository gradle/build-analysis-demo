package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.gradle.buildeng.analysis.common.DurationSerializer
import org.gradle.buildeng.analysis.common.InstantSerializer
import org.gradle.buildeng.analysis.common.NullAvoidingStringSerializer
import org.gradle.buildeng.analysis.model.*
import org.gradle.buildeng.analysis.model.BuildEvent
import java.time.Duration
import java.time.Instant

/**
 * Transforms input of the following format to JSON that is BigQuery-compatible. See https://cloud.google.com/bigquery/docs/loading-data-cloud-storage-json#limitations
 */
class BuildEventsJsonTransformer {

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
        var buildStartTimestamp: Instant? = null
        var wallClockDuration: Duration? = null
        var failureId: String? = null
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
                // TODO: understand the difference between this and the timestamp in the header
                "BuildStarted" -> buildStartTimestamp = buildEvent.timestamp
                "BuildFinished" -> {
                    wallClockDuration = Duration.between(buildStartTimestamp!!, buildEvent.timestamp)
                    failureId = buildEvent.data.path("failureId").asText()
                    failed = (failureId.isNullOrEmpty() || buildEvent.data.path("failure").asText().isNullOrEmpty())
                }
                "BuildAgent" -> buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> rootProjectName = buildEvent.data.get("rootProjectName").asText()
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

        val build = Build(buildId, rootProjectName, buildAgentId, buildRequestedTasks, buildExcludedTasks, environmentParameters, buildStartTimestamp!!, wallClockDuration!!, failureId, failed, userLinks, userNamedValues, userTags)
        return objectWriter.writeValueAsString(objectMapper.convertValue(build, JsonNode::class.java))
    }
}
