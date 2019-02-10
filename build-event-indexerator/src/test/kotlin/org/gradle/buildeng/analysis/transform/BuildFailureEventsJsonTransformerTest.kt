package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildFailureEventsJsonTransformerTest {
    @Test fun testTransformExceptionDataJson() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("failing-test-events-json.txt").file)

        val transformed = BuildFailureEventsJsonTransformer().transform(buildEventsFile.readText())
        val buildFailure = ObjectMapper().readTree(transformed)
        val exceptions = buildFailure.get("exceptions")

        assertEquals(4, exceptions.size())

        val interestingException = exceptions.find { it.get("exceptionId").asText() == "3025236424415955010" }!!

        assertEquals("org.junit.ComparisonFailure", interestingException.path("className").asText())
        assertEquals("expected:<...9-01-01 16:02:51.267[]> but was:<...9-01-01 16:02:51.267[+00]>", interestingException.path("message").asText())

        assertTrue(interestingException.get("causes").isArray)

        assertEquals(expectedTransformed, transformed)
    }
}

const val expectedTransformed = """{"buildId":"cokuz2qlzlhck","rootProjectName":"build-analysis","buildAgentId":"eric@kain.lan","timestamp":"2019-01-11 12:07:08.313-07:00","exceptions":[{"exceptionId":"3025236424415955010","className":"org.junit.ComparisonFailure","message":"expected:<...9-01-01 16:02:51.267[]> but was:<...9-01-01 16:02:51.267[+00]>","causes":[]},{"exceptionId":"3107853242888327571","className":"org.gradle.internal.exceptions.LocationAwareException","message":"Execution failed for task ':build-event-transformerator:test'.","causes":["7947411800512689240"]},{"exceptionId":"7947411800512689240","className":"org.gradle.api.tasks.TaskExecutionException","message":"Execution failed for task ':build-event-transformerator:test'.","causes":["-5019989798989744525"]},{"exceptionId":"-5019989798989744525","className":"org.gradle.api.GradleException","message":"There were failing tests. See the report at: file:///Users/eric/src/gradle/build-analysis/build-event-transformerator/build/reports/tests/test/index.html","causes":[]}]}"""
