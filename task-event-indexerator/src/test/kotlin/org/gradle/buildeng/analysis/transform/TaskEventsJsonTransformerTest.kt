package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskEventsJsonTransformerTest {
    @Test fun testTransformEventsFile() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("f23vwoax6n4uy.out").file)
        val jsonNode = ObjectMapper().readTree(TaskEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("gradle", jsonNode.get("rootProjectName").asText())

        assertTrue(jsonNode.get("tasks").isArray)
        assertEquals(912, jsonNode.get("tasks").size())
        val firstTask = jsonNode.get("tasks").get(0)

        assertEquals(":buildSrc", firstTask.get("buildPath").asText())
        assertEquals(":discoverMainScriptsExtensions", firstTask.get("path").asText())
        assertEquals("org.jetbrains.kotlin.gradle.scripting.internal.DiscoverScriptExtensionsTask", firstTask.get("className").asText())

        assertTrue(firstTask.get("executions").isArray)
        assertEquals(1, firstTask.get("executions").size())

        assertEquals("f23vwoax6n4uy", firstTask.get("executions").get(0).get("buildId").asText())
        assertEquals("tcagent1@windows25", firstTask.get("executions").get(0).get("buildAgentId").asText())
        assertEquals("2019-01-01 16:02:52.179-07:00", firstTask.get("executions").get(0).get("startTimestamp").asText())
        assertEquals("1", firstTask.get("executions").get(0).get("wallClockDuration").asText())
        assertEquals("success", firstTask.get("executions").get(0).get("outcome").asText())
    }

    @Test fun testTransformTaskStarted1_2() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("225gftys3f2ko-build-events-json.txt").file)
        val jsonNode = ObjectMapper().readTree(TaskEventsJsonTransformer().transform(buildEventsFile.readText()))

        val firstTask = jsonNode.get("tasks").first()
        assertEquals("", firstTask.get("buildPath").asText())
        assertEquals(":somethingFunky", firstTask.get("path").asText())
    }

    @Test fun testTransformUnexpectedJson() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("b6ggzkhawskus-build-events-json.txt").file)
        val jsonNode = ObjectMapper().readTree(TaskEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("gradle", jsonNode.get("rootProjectName").asText())
    }
}
