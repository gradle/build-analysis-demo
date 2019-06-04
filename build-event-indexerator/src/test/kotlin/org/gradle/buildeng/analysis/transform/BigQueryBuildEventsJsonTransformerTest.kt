package org.gradle.buildeng.analysis.transform

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BigQueryBuildEventsJsonTransformerTest {
    @Test fun testTransformHeader() {
        val jsonNode = BigQueryBuildEventsJsonTransformer()
                .transform("""{"buildId":"f23vwoax6n4uy","pluginVersion":"2.1","gradleVersion":"5.1-rc-3","timestamp":1546383767880}""")

        assertEquals("f23vwoax6n4uy", jsonNode.get("buildId").asText())
        assertEquals("2.1", jsonNode.get("pluginVersion").asText())
        assertEquals("5.1-rc-3", jsonNode.get("gradleVersion").asText())
        assertEquals("2019-01-01 23:02:47.880+00", jsonNode.get("timestamp").asText())
    }

    @Test fun testTransformEventsFile() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("gradle-build-events-json.txt").file)
        val jsonNode = BigQueryBuildEventsJsonTransformer().transform(buildEventsFile)

        assertEquals("f23vwoax6n4uy", jsonNode.get("buildId").asText())

        assertTrue(jsonNode.get("event").isArray)
        assertTrue(jsonNode.get("event").get(0).get("type").isObject)
        assertTrue(jsonNode.get("event").get(0).get("timestamp").isTextual)
        assertTrue(jsonNode.get("event").get(0).get("data").isTextual)

        assertEquals("DaemonState", jsonNode.get("event")?.get(0)?.get("type")?.get("eventType")?.asText())
        assertEquals("2019-01-01 23:02:51.267+00", jsonNode.get("event")?.get(0)?.get("timestamp")?.asText())
        assertEquals("""{"startTime":1546381961320,"buildNumber":49,"numberOfRunningDaemons":1,"idleTimeout":10800000,"singleUse":false}""", jsonNode.get("event")?.get(0)?.get("data")?.asText())

        assertEquals("BasicMemoryStats", jsonNode.get("event")?.get(2246)?.get("type")?.get("eventType")?.asText())
    }
}
