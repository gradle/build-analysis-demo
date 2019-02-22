package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transforms input of the following format to JSON that is BigQuery-compatible. See https://cloud.google.com/bigquery/docs/loading-data-cloud-storage-json#limitations
 */
class BigQueryBuildEventsJsonTransformer : JsonTransformer {
    private val objectMapper = ObjectMapper()

    override fun transform(file: File): JsonNode = transform(file.readLines())
    override fun transform(string: String): JsonNode = transform(string.split("\n"))

    override fun transform(list: List<String>): JsonNode {
        // Read first line, then everything else is events
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        val header = objectMapper.readTree(list.first())
        val events: List<BuildEvent> = list.drop(1).map {
            BuildEvent.fromJson(objectMapper.readTree(it))
        }

        val map = mapOf(
                "buildId" to header.get("buildId").asText(),
                "pluginVersion" to header.get("pluginVersion").asText(),
                "gradleVersion" to header.get("gradleVersion").asText(),
                "timestamp" to header.get("timestamp").asLong().toBigQueryDate(),
                "event" to events
        )

        return objectMapper.convertValue(map, JsonNode::class.java)
    }
}

data class BuildEvent(val type: BuildEventType, val timestamp: String, val data: String) {
    companion object {
        private val mapper = ObjectMapper()

        fun fromJson(jsonNode: JsonNode) = BuildEvent(
                BuildEventType.fromJson(mapper.convertValue(jsonNode.get("type"), JsonNode::class.java)),
                jsonNode.get("timestamp").asLong().toBigQueryDate(),
                mapper.writeValueAsString(jsonNode.get("data"))
        )
    }
}

data class BuildEventType(val eventType: String, val majorVersion: Int, val minorVersion: Int) {
    companion object {
        fun fromJson(jsonNode: JsonNode) = BuildEventType(
                jsonNode.get("eventType").asText(),
                jsonNode.get("majorVersion").asInt(),
                jsonNode.get("minorVersion").asInt())
    }
}

fun Long.toBigQueryDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS+00")
    dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    return dateFormat.format(Date(this))
}
