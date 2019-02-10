package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.BuildEvent
import org.gradle.buildeng.analysis.model.ExceptionData
import org.gradle.buildeng.analysis.model.StackFrame
import org.gradle.buildeng.analysis.model.StackTrace

class ExceptionDataEventsJsonTransformer : EventsJsonTransformer() {

    fun transform(fileContents: String): List<String> {
        val exceptions = mutableListOf<ExceptionData>()

        fileContents.split("\n")
                .filter { it.contains("\"eventType\":\"ExceptionData\"") }
                .forEach {
            val exceptionEvent = BuildEvent.fromJson(objectReader.readTree(it))!!
                    //"taskPath": ":build-event-transformerator:test"
            val stackTracesNode = exceptionEvent.data.get("stackTraces")

                    // TODO:

            exceptionEvent.data.get("exceptions").fields().asSequence().forEach { exceptionKVs ->
                exceptions.add(ExceptionData(
                        exceptionKVs.key,
                        exceptionKVs.value.path("className").asText(),
                        exceptionKVs.value.path("message").asText(),
                        stackTrace,
                        exceptionKVs.value.path("causes").map { c -> c.asText() },
                        objectWriter.writeValueAsString(exceptionKVs.value.path("metadata")),
                        exceptionKVs.value.path("classLevelAnnotations").map { c -> c.asText() }
                ))
            }
        }

        return exceptions.toList().map {
            objectWriter.writeValueAsString(objectMapper.convertValue(it, JsonNode::class.java))
        }
    }
}

