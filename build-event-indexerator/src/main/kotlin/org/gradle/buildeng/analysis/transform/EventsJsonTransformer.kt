package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.module.SimpleModule
import org.gradle.buildeng.analysis.common.DurationSerializer
import org.gradle.buildeng.analysis.common.InstantSerializer
import org.gradle.buildeng.analysis.common.NullAvoidingStringSerializer

abstract class EventsJsonTransformer {
    protected val objectMapper = ObjectMapper()
    protected val objectReader: ObjectReader = objectMapper.reader()
    protected val objectWriter: ObjectWriter = objectMapper.writer()

    init {
        objectMapper.registerModule(object : SimpleModule() {
            init {
                addSerializer(InstantSerializer())
                addSerializer(DurationSerializer())
                addSerializer(NullAvoidingStringSerializer())
            }
        })
    }

    fun transform(input: String): String {
        return transform(input.split("\n"))
    }

    abstract fun transform(list: List<String>): String

//    abstract fun transformLines(input: List<String>): String
//
//    abstract fun transformLine(input: String): String
//
//    abstract fun transformExpandLine(input: String): List<String>
}
