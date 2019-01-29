package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.TaskExecutions
import org.gradle.buildeng.analysis.transform.TaskEventsJsonTransformer


object TaskEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(TaskExecutions::class))

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .map { TaskEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable(tableId = options.output, tableSchema = tableSchema)

        pipe.run().waitUntilFinish()
    }
}
