package org.gradle.buildeng.analysis.model

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class BigQueryTableSchemaGeneratorTest {

    data class LogMessage(val id: String, val timestamp: Instant)

    data class StackTrace(val id: String, val stackFrames: List<StackFrame>)
    data class StackFrame(val frameId: String, val declaringClass: String?, val lineNumber: Int?)

    @Test fun testGenerateNonNestedJsonSchema() {
        val columns = BigQueryTableSchemaGenerator.generateFieldList(LogMessage::class)
        val jsonSchema = BigQueryTableSchemaGenerator.generateJsonSchema(columns)
        assertEquals("""[{"mode":"REQUIRED","name":"id","type":"STRING"},{"mode":"REQUIRED","name":"timestamp","type":"TIMESTAMP"}]""", jsonSchema)
    }

    @Test fun testGenerateNestedRecordSchema() {
        val columns = BigQueryTableSchemaGenerator.generateFieldList(StackTrace::class)
        val jsonSchema = BigQueryTableSchemaGenerator.generateJsonSchema(columns)
        assertEquals("""[{"mode":"REQUIRED","name":"id","type":"STRING"},{"fields":[{"mode":"NULLABLE","name":"declaringClass","type":"STRING"},{"mode":"REQUIRED","name":"frameId","type":"STRING"},{"mode":"NULLABLE","name":"lineNumber","type":"INTEGER"}],"mode":"REPEATED","name":"stackFrames","type":"RECORD"}]""", jsonSchema)
    }
}
