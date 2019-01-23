package org.gradle.buildeng.analysis.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant

data class BuildEvent(val type: BuildEventType, val timestamp: Instant, val data: JsonNode) {
    companion object {
        private val mapper = ObjectMapper()

        fun fromJson(jsonNode: JsonNode): BuildEvent? {
            if (!jsonNode.has("type")) {
                println("WARNING: Expected BuildEvent but got ${mapper.writeValueAsString(jsonNode)}")
                return null
            }

            return BuildEvent(
                    BuildEventType.fromJson(mapper.convertValue(jsonNode.get("type"), JsonNode::class.java)),
                    Instant.ofEpochMilli(jsonNode.get("timestamp").asLong()),
                    mapper.convertValue(jsonNode.get("data"), JsonNode::class.java)
            )
        }
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
