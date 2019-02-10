package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableSchema
import org.gradle.buildeng.analysis.model.BigQueryTableSchemaGenerator
import org.gradle.buildeng.analysis.model.BuildFailure
import org.gradle.buildeng.analysis.transform.BuildFailureEventsJsonTransformer


object BuildFailureEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {
        val tableSchema = TableSchema()
                .setFields(BigQueryTableSchemaGenerator.generateFieldList(BuildFailure::class))

        val (pipe, options) = KPipe.from<IndexingDataflowPipelineOptions>(args)

        pipe.fromFiles(input = options.input)
                .filter { it.value.contains("\"eventType\":\"ExceptionData\"") }
                .map { BuildFailureEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .toTable("Write to BigQuery", options.output, tableSchema)

        pipe.run().waitUntilFinish()
    }
}
