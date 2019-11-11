package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.BuildEvent
import org.gradle.buildeng.analysis.model.TaskExecution
import org.gradle.buildeng.analysis.model.TaskExecutions
import java.time.Duration
import java.time.Instant

class TaskEventsJsonTransformer : EventsJsonTransformer() {

    fun transform(fileContents: String): String {
        val list = fileContents.split("\n")
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        // Read first line, then everything else is events
        val header = objectReader.readTree(list.first())
        val buildId = header.get("buildId").asText()
        val taskExecutions = TaskExecutions(buildId = buildId, tasks = listOf(), buildTimestamp = Instant.ofEpochMilli(header.get("timestamp").asLong()))
        val tasks = mutableMapOf<String, TaskExecution>()

        val rawBuildEvents = list.drop(1)

        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            // Ignoring different build event versions here because every version has what we want
            when (buildEvent?.type?.eventType) {
                "BuildAgent" -> taskExecutions.buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> {
                    // This event is triggered for every included build, so is the only way to get root project
                    if (buildEvent.data.path("projects").any { project -> project.path("buildPath").asText() == ":" }) {
                        taskExecutions.rootProjectName = buildEvent.data.get("rootProjectName").asText()
                    }
                }
                "TaskStarted" -> {
                    val taskId = buildEvent.data.get("id").asText()
                    tasks[taskId] = TaskExecution(
                            taskId = taskId,
                            path = buildEvent.data.get("path").asText(),
                            buildPath = buildEvent.data.path("buildPath").asText(),
                            startTimestamp = buildEvent.timestamp,
                            className = buildEvent.data.path("className").asText(),
                            buildCacheInteractionIds = mutableListOf()
                    )
                }
                "TaskFinished" -> {
                    tasks[buildEvent.data.get("id").asText()]!!.apply {
                        this.wallClockDuration = Duration.between(tasks[taskId]!!.startTimestamp, buildEvent.timestamp)
                        this.outcome = buildEvent.data.path("outcome").asText()
                        this.cacheable = buildEvent.data.path("cacheable").asBoolean()
                        this.avoidedExecutionTimeMs = computeAvoidedTime(buildEvent.data.path("outcome").asText(), buildEvent.data.path("originExecutionTime").asLong())
                        this.cachingDisabledReasonCategory = buildEvent.data.path("cachingDisabledReasonCategory").asText()
                        this.actionable = buildEvent.data.path("actionable").asBoolean()
                    }
                }
                "BuildCachePackStarted", "BuildCacheUnpackStarted", "BuildCacheRemoteLoadStarted", "BuildCacheRemoteStoreStarted" -> {
                    val taskExecution = tasks[buildEvent.data.get("task").asText()]
                    taskExecution?.buildCacheInteractionIds?.add(buildEvent.data.get("id").asText())
                }
            }
        }

        val outputJson = objectMapper.convertValue(taskExecutions.copy(tasks = tasks.values.toList()), JsonNode::class.java)
        return objectWriter.writeValueAsString(outputJson)
    }

    private fun computeAvoidedTime(outcome: String, originExecutionTime: Long?): Long? {
        if (outcome == "up_to_date" || outcome == "from_cache") {
            return originExecutionTime
        }
        return null
    }
}
