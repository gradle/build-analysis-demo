package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.transform.BuildEventsJsonTransformer
import java.util.*


object BuildEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {

        val fieldSchema = ArrayList<TableFieldSchema>()
        fieldSchema.add(TableFieldSchema().setName("buildId").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("rootProjectName").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("buildAgentId").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("buildRequestedTasks").setType("STRING").setMode("REPEATED"))
        fieldSchema.add(TableFieldSchema().setName("buildExcludedTasks").setType("STRING").setMode("REPEATED"))
        fieldSchema.add(TableFieldSchema().setName("environmentParameters").setType("RECORD").setMode("REPEATED").setFields(listOf(
                TableFieldSchema().setName("key").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("value").setType("STRING").setMode("REQUIRED")
        )))
        fieldSchema.add(TableFieldSchema().setName("startTimestamp").setType("TIMESTAMP").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("wallClockDuration").setType("INTEGER").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("failureId").setType("STRING").setMode("NULLABLE"))
        fieldSchema.add(TableFieldSchema().setName("failed").setType("BOOLEAN").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("userLink").setType("RECORD").setMode("REPEATED").setFields(listOf(
                TableFieldSchema().setName("label").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("url").setType("STRING").setMode("REQUIRED")
        )))
        fieldSchema.add(TableFieldSchema().setName("userNamedValue").setType("RECORD").setMode("REPEATED").setFields(listOf(
                TableFieldSchema().setName("key").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("value").setType("STRING").setMode("REQUIRED")
        )))
        fieldSchema.add(TableFieldSchema().setName("userTag").setType("STRING").setMode("REPEATED"))
        val tableSchema = TableSchema()
        tableSchema.fields = fieldSchema

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .map { BuildEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable(tableId = options.output, tableSchema = tableSchema)

        pipe.run().waitUntilFinish()
    }
}
