package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.ExceptionData
import org.gradle.buildeng.analysis.transform.ExceptionDataEventsJsonTransformer


object ExceptionEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(ExceptionData::class))

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .filter { it.value.contains("\"eventType\":\"ExceptionData\"") }
                .flatMap { ExceptionDataEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable("Write to BigQuery", options.output, tableSchema)

        pipe.run().waitUntilFinish()
    }
}
