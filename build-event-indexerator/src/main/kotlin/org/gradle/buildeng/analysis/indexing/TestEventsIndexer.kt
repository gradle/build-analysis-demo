package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.transform.TestEventsJsonTransformer
import java.util.*


object TestEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {

        val fieldSchema = ArrayList<TableFieldSchema>()
        fieldSchema.add(TableFieldSchema().setName("rootProjectName").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("tests").setType("RECORD").setMode("REPEATED").setFields(listOf(
                TableFieldSchema().setName("suite").setType("BOOLEAN").setMode("REQUIRED"),
                TableFieldSchema().setName("className").setType("STRING").setMode("NULLABLE"),
                TableFieldSchema().setName("name").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("taskId").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("executions").setType("RECORD").setMode("REPEATED").setFields(listOf(
                        TableFieldSchema().setName("buildId").setType("STRING").setMode("REQUIRED"),
                        TableFieldSchema().setName("buildAgentId").setType("STRING").setMode("REQUIRED"),
                        TableFieldSchema().setName("startTimestamp").setType("TIMESTAMP").setMode("REQUIRED"),
                        TableFieldSchema().setName("wallClockDuration").setType("INTEGER").setMode("REQUIRED"),
                        TableFieldSchema().setName("skipped").setType("BOOLEAN").setMode("REQUIRED"),
                        TableFieldSchema().setName("failed").setType("BOOLEAN").setMode("REQUIRED"),
                        TableFieldSchema().setName("failureId").setType("STRING").setMode("NULLABLE"),
                        TableFieldSchema().setName("failure").setType("STRING").setMode("NULLABLE")
                ))
        )))
        val tableSchema = TableSchema()
        tableSchema.fields = fieldSchema

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .filter { it.value.contains("\"eventType\":\"TestStarted\"") }
                .map { TestEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable(tableId = options.output, tableSchema = tableSchema)

        pipe.run().waitUntilFinish()
    }
}
