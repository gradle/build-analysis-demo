package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import com.google.api.services.bigquery.model.TimePartitioning
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.BuildCacheInteraction
import org.gradle.buildeng.analysis.transform.BuildCacheEventsJsonTransformer


object BuildCacheEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(BuildCacheInteraction::class))
        val timePartitioning = TimePartitioning().setField(BuildCacheInteraction::startTimestamp.name)

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .filter { it.value.contains("\"eventType\":\"BuildCache") }
                .flatMap { BuildCacheEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable("Write to BigQuery", options.output, tableSchema, timePartitioning)

        pipe.run().waitUntilFinish()
    }
}
