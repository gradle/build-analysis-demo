package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.gradle.buildeng.analysis.common.DurationSerializer
import org.gradle.buildeng.analysis.common.InstantSerializer
import org.gradle.buildeng.analysis.model.Project
import org.gradle.buildeng.analysis.model.Task
import org.gradle.buildeng.analysis.model.TaskExecution
import java.time.Duration
import java.time.Instant

/**
 * Transforms input of the following format to JSON that is BigQuery-compatible. See https://cloud.google.com/bigquery/docs/loading-data-cloud-storage-json#limitations
 */
class TaskEventsJsonTransformer {

    private val objectMapper = ObjectMapper()
    private val objectReader = objectMapper.reader()
    private val objectWriter = objectMapper.writer()

    init {
        objectMapper.registerModule(object : SimpleModule() {
            init {
                addSerializer(InstantSerializer())
                addSerializer(DurationSerializer())
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

        val rawBuildEvents = list.drop(1)
        val taskEvents = mutableListOf<BuildEvent>()

        // TODO: make this prettier/more functional
        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            // Ignoring different build event versions here because every version has what we want
            when (buildEvent?.type?.eventType) {
                "BuildAgent" -> buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> rootProjectName = buildEvent.data.get("rootProjectName").asText()
                "TaskStarted" -> taskEvents.add(buildEvent)
                "TaskFinished" -> taskEvents.add(buildEvent)
                // TODO: handle BuildCacheUnpackStarted, BuildCacheRemoteLoadStarted, BuildCachePackStarted/Finished
            }
        }

        // Pair up task started and finished events by ID
        val tasks = mutableMapOf<Long, Task>()
        taskEvents.forEach { buildEvent ->
            when (buildEvent.type.eventType) {
                "TaskStarted" -> {
                    // NOTE: using data.path(...) here to avoid NPEs caused by missing properties for old eventType versions
                    val task = Task(
                            buildEvent.data.path("buildPath").asText(),
                            buildEvent.data.path("path").asText(),
                            buildEvent.data.path("className").asText(),
                            listOf(TaskExecution(buildId, buildAgentId, buildEvent.timestamp, Duration.ZERO, "UNKNOWN"))
                    )
                    tasks[buildEvent.data.get("id").asLong()] = task
                }
                "TaskFinished" -> {
                    val buildEventId = buildEvent.data.get("id").asLong()
                    val startedTask: Task = tasks[buildEventId]!!
                    val duration = Duration.between(startedTask.executions.first().startTimestamp, buildEvent.timestamp)
                    val taskExecution = startedTask.executions.first()
                            .copy(wallClockDuration = duration, outcome = buildEvent.data.get("outcome").asText())
                    tasks[buildEventId] = startedTask.copy(executions = listOf(taskExecution))
                }
            }
        }

        val project = Project(rootProjectName, tasks.values.toList())
        return objectWriter.writeValueAsString(objectMapper.convertValue(project, JsonNode::class.java))
    }
}

data class BuildEvent(val type: BuildEventType, val timestamp: Instant, val data: JsonNode) {
    companion object {
        private val mapper = ObjectMapper()

        fun fromJson(jsonNode: JsonNode): BuildEvent? {
            if (!jsonNode.has("type")) {
                println("WARNING: Expected BuildEvent but got ${mapper.writeValueAsString(jsonNode)}")
                return null
            }

            return BuildEvent(
                    BuildEventType.fromJson(mapper.convertValue(jsonNode.get("type"), JsonNode::class.java)),
                    Instant.ofEpochMilli(jsonNode.get("timestamp").asLong()),
                    mapper.convertValue(jsonNode.get("data"), JsonNode::class.java)
            )
        }
    }
}

data class BuildEventType(val eventType: String, val majorVersion: Int, val minorVersion: Int) {
    companion object {
        fun fromJson(jsonNode: JsonNode) = BuildEventType(
                jsonNode.get("eventType").asText(),
                jsonNode.get("majorVersion").asInt(),
                jsonNode.get("minorVersion").asInt())
    }
}
