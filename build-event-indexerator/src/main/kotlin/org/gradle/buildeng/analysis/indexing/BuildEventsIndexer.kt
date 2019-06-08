package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import com.google.api.services.bigquery.model.TimePartitioning
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.Build
import org.gradle.buildeng.analysis.transform.BuildEventsJsonTransformer


object BuildEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(Build::class))
        val timePartitioning = TimePartitioning().setField(Build::buildTimestamp.name)

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .filter { it.value.length > 300 }
                .map { BuildEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable("Write to BigQuery", options.output, tableSchema, timePartitioning)

        pipe.run().waitUntilFinish()
    }
}
