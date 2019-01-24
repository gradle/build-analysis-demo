package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionDataEventsJsonTransformerTest {
    @Test fun testTransformExceptionDataJson() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("failing-test-events-json.txt").file)

        val input = buildEventsFile.readText().split("\n").find { it.contains("ExceptionData") }!!
        val exceptionObjects = ExceptionDataEventsJsonTransformer().transform(input).map {
            ObjectMapper().readTree(it)
        }

        assertEquals(4, exceptionObjects.size)

        val interestingException = exceptionObjects.find { it.get("exceptionId").asText() == "3025236424415955010" }!!

        assertEquals("org.junit.ComparisonFailure", interestingException.path("className").asText())
        assertEquals("expected:<...9-01-01 16:02:51.267[]> but was:<...9-01-01 16:02:51.267[+00]>", interestingException.path("message").asText())

        assertTrue(interestingException.get("causes").isArray)
        assertEquals("{}", interestingException.path("metadata").asText())

        val stackTrace = interestingException.path("stackTrace")
        assertTrue(stackTrace.isObject)

        assertEquals("7944817797209700127", stackTrace.get("stackTraceId").asText())
        assertTrue(stackTrace.get("stackFrames").isArray)
        assertEquals(54, stackTrace.get("stackFrames").size())

        val firstStackFrame = stackTrace.get("stackFrames").get(0)
        assertEquals("2410246218360306666", firstStackFrame.get("stackFrameId").asText())
        assertEquals("org.junit.Assert", firstStackFrame.get("declaringClass").asText())
        assertEquals("assertEquals", firstStackFrame.get("methodName").asText())
        assertEquals("Assert.java", firstStackFrame.get("fileName").asText())
        assertEquals(115, firstStackFrame.get("lineNumber").asInt())
        assertEquals("", firstStackFrame.get("fileRef").asText())
    }
}
