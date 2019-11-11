package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskEventsJsonTransformerTest {
    @Test fun testTransformEventsFile() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("gradle-build-events-json.txt").file)
        val jsonNode = ObjectMapper().readTree(TaskEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("f23vwoax6n4uy", jsonNode.get("buildId").asText())
        assertEquals("gradle", jsonNode.get("rootProjectName").asText())
        assertEquals("tcagent1@windows25", jsonNode.get("buildAgentId").asText())

        assertTrue(jsonNode.get("tasks").isArray)
        assertEquals(912, jsonNode.get("tasks").size())
        val firstTask = jsonNode.get("tasks").get(0)

        assertEquals(":buildSrc", firstTask.get("buildPath").asText())
        assertEquals(":discoverMainScriptsExtensions", firstTask.get("path").asText())
        assertEquals("org.jetbrains.kotlin.gradle.scripting.internal.DiscoverScriptExtensionsTask", firstTask.get("className").asText())
        assertEquals("2019-01-01 23:02:52.179+00", firstTask.get("startTimestamp").asText())
        assertEquals("1", firstTask.get("wallClockDuration").asText())
        assertEquals("success", firstTask.get("outcome").asText())
        assertEquals(0, firstTask.get("avoidedExecutionTimeMs").asLong())
    }

    @Test fun testTransformTaskStarted1_2() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("build-scan-v1x-build-events-json.txt").file)
        val jsonNode = ObjectMapper().readTree(TaskEventsJsonTransformer().transform(buildEventsFile.readText()))

        val firstTask = jsonNode.get("tasks").first()
        assertEquals("", firstTask.get("buildPath").asText())
        assertEquals(":somethingFunky", firstTask.get("path").asText())
    }

    @Test fun testTransformUnexpectedJson() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("bogus-malformed-build-events-json.txt").file)
        val jsonNode = ObjectMapper().readTree(TaskEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("gradle", jsonNode.get("rootProjectName").asText())
    }
}
