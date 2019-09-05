package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

fun ObjectMapper.prettyPrint(json: JsonNode): String =
        writerWithDefaultPrettyPrinter()
                .writeValueAsString(json)
                .replace("\r\n", "\n")
