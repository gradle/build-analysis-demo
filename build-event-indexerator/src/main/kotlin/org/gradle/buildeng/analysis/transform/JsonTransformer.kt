package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import java.io.File

interface JsonTransformer {
    fun transform(file: File): JsonNode
    fun transform(string: String): JsonNode
    fun transform(list: List<String>): JsonNode
}
