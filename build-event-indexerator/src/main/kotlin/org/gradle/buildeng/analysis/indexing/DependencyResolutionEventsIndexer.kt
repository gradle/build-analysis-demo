package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import com.google.api.services.bigquery.model.TimePartitioning
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.DependencyResolution
import org.gradle.buildeng.analysis.transform.DependencyResolutionEventsJsonTransformer


object DependencyResolutionEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(DependencyResolution::class))
        val timePartitioning = TimePartitioning().setField(DependencyResolution::buildTimestamp.name)

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .map { DependencyResolutionEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable("Write to BigQuery", options.output, tableSchema, timePartitioning)

        pipe.run().waitUntilFinish()
    }
}
