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
            val stackTracesNode = exceptionEvent.data.get("stackTraces")
            val stackFramesNode = exceptionEvent.data.get("stackFrames")

            exceptionEvent.data.get("exceptions").fields().asSequence().forEach { exceptionKVs ->
                val stackTraceNode = stackTracesNode.get(exceptionKVs.value.get("stackTrace").asText())
                val stackFrameIds = stackTraceNode.get("stackFrames")
                        .map { stackFrameNode ->
                            val stackFrameId = stackFrameNode.asText()
                            val jsonNode = stackFramesNode.get(stackFrameNode.asText())
                            StackFrame(
                                    stackFrameId,
                                    jsonNode.path("declaringClass").asText(),
                                    jsonNode.path("methodName").asText(),
                                    jsonNode.path("fileName").asText(),
                                    jsonNode.path("lineNumber").asInt(),
                                    jsonNode.path("fileRef").asText())
                        }

                val stackTrace = StackTrace(exceptionKVs.value.path("stackTrace").asText(), stackFrameIds)

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

