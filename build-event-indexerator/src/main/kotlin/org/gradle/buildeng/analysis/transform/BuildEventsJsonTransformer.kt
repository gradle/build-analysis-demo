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

        var buildTool = "UNKNOWN_BUILD_TOOL"
        var buildAgentId = "UNKNOWN_BUILD_AGENT_ID"
        var rootProjectName = "UNKNOWN_ROOT_PROJECT"
        val buildId = header.get("buildId").asText()
        val buildToolVersion = header.get("gradleVersion").asText()
        var buildStartTimestamp = Instant.ofEpochMilli(header.get("timestamp").asLong())
        // TODO: measure wall clock duration more accurately given a build that never sends a BuildFinished event. Or perhaps exclude it.
        var wallClockDuration: Duration = Duration.ZERO
        var failureIds = listOf<String>()
        var failed = false
        var failureData: FailureData? = null

        val rawBuildEvents = list.drop(1)
        val buildRequestedTasks = mutableListOf<String>()
        val buildExcludedTasks = mutableListOf<String>()
        val environmentParameters = mutableListOf<EnvironmentParameter>()
        val goalAndTaskExecutions = mutableMapOf<String, TaskOrGoalExecution>()
        val userLinks = mutableListOf<UserLink>()
        val userNamedValues = mutableListOf<UserNamedValue>()
        val userTags = mutableListOf<String>()

        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            when (buildEvent?.type?.eventType) {
                // NOTE: header (build) timestamp represents when build scan was received, so make sure we use buildStart timestamp to measure build duration
                "BuildStarted" -> {
                    buildTool = "Gradle"
                    buildStartTimestamp = buildEvent.timestamp
                }
                "MvnBuildStarted" -> {
                    buildTool = "Maven"
                    buildStartTimestamp = buildEvent.timestamp
                }
                "BuildFinished" -> {
                    wallClockDuration = Duration.between(buildStartTimestamp, buildEvent.timestamp)
                    val maybeFailureId = buildEvent.data.path("failureId")
                    if (!maybeFailureId.isNull) {
                        failureIds = listOf(maybeFailureId.asText())
                        failed = true
                    }
                }
                "MvnBuildFinished" -> {
                    wallClockDuration = Duration.between(buildStartTimestamp, buildEvent.timestamp)
                    failureIds = buildEvent.data.get("failureIds").map { id -> id.asText() }
                    failed = failureIds.isNotEmpty()
                }
                "BuildAgent", "MvnBuildAgent" -> buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> {
                    // This event is triggered for every included build, so is the only way to get root project
                    if (buildEvent.data.path("projects").any { project -> project.path("buildPath").asText() == ":" }) {
                        rootProjectName = buildEvent.data.get("rootProjectName").asText()
                    }
                }
                "MvnProjectStructure" -> {
                    // From https://docs.gradle.com/enterprise/event-model-javadoc/: "The first project is guaranteed to be the top level project."
                    rootProjectName = buildEvent.data.path("projects")[0].path("artifactId").asText()
                }
                "BuildRequestedTasks" -> {
                    buildRequestedTasks.addAll(buildEvent.data.get("requested").map { arg -> arg.asText() })
                    buildExcludedTasks.addAll(buildEvent.data.get("excluded").map { arg -> arg.asText() })
                }
                "MvnBuildRequestedGoals" -> {
                    buildRequestedTasks.addAll(buildEvent.data.get("goals").map { arg -> arg.asText() })
                }
                "BuildModes", "DaemonState", "Hardware", "Os", "Jvm", "JvmArgs", "BasicMemoryStats", "Locality", "Encoding", "ScopeIds", "FileRefRoots",
                "MvnHardware", "MvnOs", "MvnJvm", "MvnBasicMemoryStats", "MvnLocality", "MvnEncoding", "MvnScopeIds", "MvnFileRefRoots" -> {
                    val environmentData = objectWriter.writeValueAsString(buildEvent.data)
                    environmentParameters.add(EnvironmentParameter(buildEvent.type.eventType, environmentData))
                }
                "TaskStarted" -> {
                    val taskPath = buildEvent.data.get("path").asText()
                    goalAndTaskExecutions[taskPath] = TaskOrGoalExecution(
                            id = buildEvent.data.get("id").asText(),
                            name = taskPath,
                            className = buildEvent.data.path("className").asText()
                    )
                }
                "MvnGoalExecutionStarted" -> {
                    val goalId = buildEvent.data.get("id").asText()
                    goalAndTaskExecutions[goalId] = TaskOrGoalExecution(
                            id = goalId,
                            name = buildEvent.data.path("name").asText(),
                            className = buildEvent.data.path("mojoClassName").asText()
                    )
                }
                "MvnGoalExecutionFinished" -> {
                    val goalFailureId = buildEvent.data.path("failureId").asText()
                    if (goalFailureId != null) {
                        goalAndTaskExecutions[buildEvent.data.get("id").asText()]!!.apply {
                            this.failureId = goalFailureId
                        }
                    }
                }
                "UserLink", "MvnUserLink" -> userLinks.add(UserLink(buildEvent.data.get("label").asText(), buildEvent.data.get("url").asText()))
                "UserNamedValue", "MvnUserNamedValue" -> userNamedValues.add(UserNamedValue(buildEvent.data.get("key").asText(), buildEvent.data.get("value").asText()))
                "UserTag", "MvnUserTag" -> userTags.add(buildEvent.data.get("tag").asText())

                "ExceptionData" -> {
                    val causes = buildEvent.data.get("exceptions").fields().asSequence().map { exceptionKVs ->
                        val failedTaskPath = exceptionKVs.value.path("metadata").path("taskPath").asText()
                        var failedTaskType: String? = null
                        val failedGradleExecution = goalAndTaskExecutions[failedTaskPath]
                        if (failedGradleExecution != null) {
                            failedTaskType = failedGradleExecution.className
                        }
                        ExceptionData(
                                exceptionKVs.key,
                                exceptionKVs.value.path("className").asText(),
                                exceptionKVs.value.path("message").asText(),
                                failedTaskPath,
                                failedTaskType
                        )
                    }.toList()

                    failureData = FailureData(FailureClassifier.classifyFailure(causes).name, causes)
                }
                "MvnExceptionData" -> {
                    val causes = buildEvent.data.get("exceptions").fields().asSequence().map { exceptionKVs ->
                        val failedGoal: TaskOrGoalExecution? = goalAndTaskExecutions.values.find { goalExecution -> goalExecution.failureId == exceptionKVs.key }
                        var failedGoalName: String? = null
                        var failedMojoClassName: String? = null
                        if (failedGoal != null) {
                            failedGoalName = failedGoal.name
                            failedMojoClassName = failedGoal.className
                        }

                        ExceptionData(
                                exceptionKVs.key,
                                exceptionKVs.value.path("className").asText(),
                                exceptionKVs.value.path("message").asText(),
                                failedGoalName,
                                failedMojoClassName
                        )
                    }.toList()

                    failureData = FailureData(FailureClassifier.classifyFailure(causes).name, causes)
                }
            }
        }

        val build = Build(buildId, rootProjectName, buildTool, buildToolVersion, buildAgentId, buildRequestedTasks, buildExcludedTasks, environmentParameters, buildStartTimestamp, wallClockDuration, failureIds, failed, failureData, userLinks, userNamedValues, userTags)
        return objectWriter.writeValueAsString(objectMapper.convertValue(build, JsonNode::class.java))
    }
}
