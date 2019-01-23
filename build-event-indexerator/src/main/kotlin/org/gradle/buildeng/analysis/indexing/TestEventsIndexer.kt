package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.io.FileIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder
import org.apache.beam.sdk.options.Default
import org.apache.beam.sdk.options.Description
import org.apache.beam.sdk.options.Validation
import org.apache.beam.sdk.transforms.Filter
import org.apache.beam.sdk.transforms.MapElements
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.TypeDescriptors.kvs
import org.apache.beam.sdk.values.TypeDescriptors.strings
import org.gradle.buildeng.analysis.transform.TestEventsJsonTransformer
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*


object TestEventsIndexer {
    @JvmStatic
    fun main(args: Array<String>) {

        val fieldSchema = ArrayList<TableFieldSchema>()
        fieldSchema.add(TableFieldSchema().setName("rootProjectName").setType("STRING").setMode("REQUIRED"))
        fieldSchema.add(TableFieldSchema().setName("tests").setType("RECORD").setMode("REPEATED").setFields(listOf(
                TableFieldSchema().setName("suite").setType("BOOLEAN").setMode("REQUIRED"),
                TableFieldSchema().setName("className").setType("STRING").setMode("NULLABLE"),
                TableFieldSchema().setName("name").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("taskId").setType("STRING").setMode("REQUIRED"),
                TableFieldSchema().setName("executions").setType("RECORD").setMode("REPEATED").setFields(listOf(
                        TableFieldSchema().setName("buildId").setType("STRING").setMode("REQUIRED"),
                        TableFieldSchema().setName("buildAgentId").setType("STRING").setMode("REQUIRED"),
                        TableFieldSchema().setName("startTimestamp").setType("TIMESTAMP").setMode("REQUIRED"),
                        TableFieldSchema().setName("wallClockDuration").setType("INTEGER").setMode("REQUIRED"),
                        TableFieldSchema().setName("skipped").setType("BOOLEAN").setMode("REQUIRED"),
                        TableFieldSchema().setName("failed").setType("BOOLEAN").setMode("REQUIRED"),
                        TableFieldSchema().setName("failureId").setType("STRING").setMode("NULLABLE"),
                        TableFieldSchema().setName("failure").setType("STRING").setMode("NULLABLE")
                ))
        )))
        val tableSchema = TableSchema()
        tableSchema.fields = fieldSchema

        val (pipe, options) = KPipe.from<TaskEventsTransformerOptions>(args)

        // TODO: either eliminate KPipe or make it useful for all these .apply()
        pipe.apply(FileIO.match().filepattern(options.inputFilePattern))
                .apply(FileIO.readMatches())
                .apply(MapElements.into(kvs(strings(), strings())).via(SerializableFunction { file: FileIO.ReadableFile ->
                    KV.of(file.metadata.resourceId().toString(), file.readFullyAsUTF8String())
                }))
                .apply("Filter only builds with tests", Filter.by(SerializableFunction { it.value.contains("\"eventType\":\"TestStarted\"") }))
                .map { TestEventsJsonTransformer().transform(it.value) }
                .map { convertJsonToTableRow(it)!! }
                .apply("Write to BigQuery", BigQueryIO.writeTableRows()
                        .withExtendedErrorInfo()
                        .withSchema(tableSchema)
                        .to(options.output)
                        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
                )

        pipe.run().waitUntilFinish()
    }

    /**
     * Converts a JSON string to a [TableRow] object. If the data fails to convert, a RuntimeException will be thrown.
     *
     * @param json The JSON string to parse.
     * @return The parsed [TableRow] object.
     */
    private fun convertJsonToTableRow(json: String): TableRow? {
        // TODO: avoid converting to JSON => String => JSON again
        var row: TableRow? = null
        try {
            ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8)).use { inputStream ->
                row = TableRowJsonCoder.of().decode(inputStream, Coder.Context.OUTER)
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to serialize json to table row: $json", e)
        }

        return row
    }

    interface TaskEventsTransformerOptions : DataflowPipelineOptions {
        // TODO: allow this input to be specified on the CLI
        @get:Description("The GCS location of the text you'd like to process")
        @get:Default.String("gs://gradle-task-test-cache-events-raw/*.txt")
        var inputFilePattern: String

        @get:Description("Output table to write to")
        @get:Validation.Required
        var output: String
    }
}
