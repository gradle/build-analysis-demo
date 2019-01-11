package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildEventTypeTest {
    @Test fun testFromJson() {
        val input = ObjectMapper().readTree("""{"majorVersion":1,"minorVersion":2,"eventType":"DaemonState"}""")
        val buildEventType = BuildEventType.fromJson(input)

        assertEquals(1, buildEventType.majorVersion)
        assertEquals(2, buildEventType.minorVersion)
        assertEquals("DaemonState", buildEventType.eventType)
    }
}
