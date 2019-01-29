package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.TestsContainer
import org.gradle.buildeng.analysis.transform.TestEventsJsonTransformer


object TestEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(TestsContainer::class))

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .filter { it.value.contains("\"eventType\":\"TestStarted\"") }
                .map { TestEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable(tableId = options.output, tableSchema = tableSchema)

        pipe.run().waitUntilFinish()
    }
}
