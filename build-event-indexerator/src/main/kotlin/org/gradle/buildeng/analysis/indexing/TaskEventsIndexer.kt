package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.transform.TaskEventsJsonTransformer
import java.util.*


object TaskEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {

        val fieldSchema = ArrayList<TableFieldSchema>()
        fieldSchema.add(TableFieldSchema().setName("buildId").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("rootProjectName").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("buildAgentId").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("tasks").setType("RECORD").setMode("REPEATED").setFields(listOf(
                TableFieldSchema().setName("taskId").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("path").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("startTimestamp").setType("TIMESTAMP").setMode("REQUIRED"),
                TableFieldSchema().setName("wallClockDuration").setType("INTEGER").setMode("NULLABLE"),
                TableFieldSchema().setName("buildPath").setType("STRING").setMode("NULLABLE"),
                TableFieldSchema().setName("className").setType("STRING").setMode("NULLABLE"),
                TableFieldSchema().setName("outcome").setType("STRING").setMode("NULLABLE"),
                TableFieldSchema().setName("cacheable").setType("BOOLEAN").setMode("NULLABLE"),
                TableFieldSchema().setName("cachingDisabledReasonCategory").setType("STRING").setMode("NULLABLE"),
                TableFieldSchema().setName("actionable").setType("BOOLEAN").setMode("NULLABLE"),
                TableFieldSchema().setName("buildCacheInteractionIds").setType("STRING").setMode("REPEATED")
        )))
        val tableSchema = TableSchema()
        tableSchema.fields = fieldSchema

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .map { TaskEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable(tableId = options.output, tableSchema = tableSchema)

        pipe.run().waitUntilFinish()
    }
}
