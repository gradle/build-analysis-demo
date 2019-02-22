package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class BuildCacheEventsJsonTransformerTest {
    @Test fun testTransformBuildCacheEventsJson() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("build-cache-events-json.txt").file)

        val buildCacheInteractions = BuildCacheEventsJsonTransformer().transform(buildEventsFile.readText()).map { ObjectMapper().readTree(it) }

        assertEquals(245, buildCacheInteractions.size)

        val interactionJson = buildCacheInteractions.find { it.get("id").asText() == "-6637158154480190104" }!!

        assertEquals("22kewjtum4czo", interactionJson.path("buildId").asText())
        assertEquals("-5109164361004395213", interactionJson.path("taskId").asText())
        assertEquals("BuildCacheUnpack", interactionJson.path("type").asText())
        assertEquals("e914e0d77f8cd8a43576751dda28d9c7", interactionJson.path("cacheKey").asText())
        assertEquals("2019-01-01 22:54:44.081+00", interactionJson.path("startTimestamp").asText())
        assertEquals(0, interactionJson.path("duration").asInt())
        assertEquals("", interactionJson.path("failureId").asText())
    }
}
