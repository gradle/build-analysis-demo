package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildEventTest {
    @Test
    fun testFromJson() {
        val input = ObjectMapper().readTree("""{"timestamp":1546383771267,"type":{"majorVersion":1,"minorVersion":0,"eventType":"Hardware"},"data":{"numProcessors":4}}""")
        val buildEvent = BuildEvent.fromJson(input)

        assertEquals("2019-01-01 23:02:51.267+00", buildEvent.timestamp)
        assertEquals("{\"numProcessors\":4}", buildEvent.data)

        assertEquals(1, buildEvent.type.majorVersion)
        assertEquals(0, buildEvent.type.minorVersion)
        assertEquals("Hardware", buildEvent.type.eventType)
    }
}
