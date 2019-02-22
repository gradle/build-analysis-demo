package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestEventsJsonTransformerTest {
    @Test fun testTransformFailingTestEvent() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("failing-test-events-json.txt").file)
        val jsonNode = ObjectMapper().readTree(TestEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("build-analysis", jsonNode.get("rootProjectName").asText())
        assertEquals("cokuz2qlzlhck", jsonNode.get("buildId").asText())
        assertEquals("eric@kain.lan", jsonNode.get("buildAgentId").asText())

        assertTrue(jsonNode.get("tests").isArray)
        assertEquals(8, jsonNode.get("tests").size())
        val firstTest = jsonNode.get("tests").get(5)

        assertEquals(false, firstTest.get("suite").asBoolean())
        assertEquals("org.gradle.buildeng.analysis.transform.BuildEventTest", firstTest.get("className").asText())
        assertEquals("testFromJson", firstTest.get("name").asText())
        assertEquals(":build-event-transformerator:test", firstTest.get("taskId").asText())

        assertTrue(firstTest.get("executions").isArray)
        assertEquals(1, firstTest.get("executions").size())

        assertEquals("2019-01-11 19:07:32.982+00", firstTest.get("executions").get(0).get("startTimestamp").asText())
        assertEquals(36, firstTest.get("executions").get(0).get("wallClockDuration").asInt())
        assertEquals(false, firstTest.get("executions").get(0).get("skipped").asBoolean())
        assertEquals(true, firstTest.get("executions").get(0).get("failed").asBoolean())
        assertEquals("3025236424415955010", firstTest.get("executions").get(0).get("failureId").asText())
        assertEquals("", firstTest.get("executions").get(0).get("failure").asText())
    }
}
