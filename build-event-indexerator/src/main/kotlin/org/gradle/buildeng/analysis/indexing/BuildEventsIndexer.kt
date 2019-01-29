package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.Build
import org.gradle.buildeng.analysis.transform.BuildEventsJsonTransformer


object BuildEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(Build::class))

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .map { BuildEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable(tableId = options.output, tableSchema = tableSchema)

        pipe.run().waitUntilFinish()
    }
}
