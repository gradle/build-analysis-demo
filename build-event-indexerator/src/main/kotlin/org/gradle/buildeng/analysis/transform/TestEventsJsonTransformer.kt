package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.BuildEvent
import org.gradle.buildeng.analysis.model.Test
import org.gradle.buildeng.analysis.model.TestExecution
import org.gradle.buildeng.analysis.model.TestsContainer
import java.time.Duration
import java.time.Instant

class TestEventsJsonTransformer : EventsJsonTransformer() {

    fun transform(fileContents: String): String {
        val list = fileContents.split("\n")
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        // Read first line, then everything else is events
        val header = objectReader.readTree(list.first())

        var buildAgentId = "UNKNOWN_BUILD_AGENT_ID"
        var rootProjectName = "UNKNOWN_ROOT_PROJECT"
        val buildId = header.get("buildId").asText()
        val buildTimestamp = Instant.ofEpochMilli(header.get("timestamp").asLong())

        val rawBuildEvents = list.drop(1)
        val testEvents = mutableListOf<BuildEvent>()
        val taskIdToPath = mutableMapOf<String, String>()

        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            // Ignoring different build event versions here because every version has what we want
            when (buildEvent?.type?.eventType) {
                "BuildAgent" -> buildAgentId = "${buildEvent.data.get("username").asText()}@${buildEvent.data.path("publicHostname").asText()}"
                "ProjectStructure" -> {
                    // This event is triggered for every included build, so is the only way to get root project
                    if (buildEvent.data.path("projects").any { project -> project.path("buildPath").asText() == ":" }) {
                        rootProjectName = buildEvent.data.get("rootProjectName").asText()
                    }
                }
                "TaskStarted" -> taskIdToPath[buildEvent.data.get("id").asText()] = buildEvent.data.get("path").asText()
                "TestStarted" -> testEvents.add(buildEvent)
                "TestFinished" -> testEvents.add(buildEvent)
            }
        }

        // Pair up test started and finished events by ID
        val tests = mutableMapOf<Long, Test>()
        testEvents.forEach { buildEvent ->
            when (buildEvent.type.eventType) {
                "TestStarted" -> {
                    val test = Test(
                            buildEvent.data.get("suite").asBoolean(),
                            buildEvent.data.get("className").asText(),
                            buildEvent.data.get("name").asText(),
                            taskIdToPath[buildEvent.data.get("task").asText()].orEmpty(),
                            listOf(TestExecution(buildEvent.timestamp, Duration.ZERO, false, false, null, null))
                    )
                    tests[buildEvent.data.get("id").asLong()] = test
                }
                "TestFinished" -> {
                    val buildEventId = buildEvent.data.get("id").asLong()
                    val startedTest: Test = tests[buildEventId]!!
                    val duration = Duration.between(startedTest.executions.first().startTimestamp, buildEvent.timestamp)
                    val failed = buildEvent.data.get("failed").asBoolean()
                    val skipped = buildEvent.data.get("skipped").asBoolean()
                    // NOTE: using data.path(...) here to avoid NPEs caused by missing failureId in TestFinished_1_1
                    val failureId = buildEvent.data.path("failureId").asText()
                    val failure = buildEvent.data.get("failure").asText()
                    val testExecution = startedTest.executions.first()
                            .copy(wallClockDuration = duration, failed = failed, skipped = skipped, failureId = failureId, failure = failure)
                    tests[buildEventId] = startedTest.copy(executions = listOf(testExecution))
                }
            }
        }

        val project = TestsContainer(rootProjectName, buildId, buildTimestamp, buildAgentId, tests.values.toList())
        return objectWriter.writeValueAsString(objectMapper.convertValue(project, JsonNode::class.java))
    }
}

