package org.gradle.buildeng.data.model

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

data class EventType(val eventType: String?, val majorVersion: Short = 0, val minorVersion: Short = 0)

data class ExportEvent(var timestamp: Instant, var type: EventType, val data: JsonNode?)
