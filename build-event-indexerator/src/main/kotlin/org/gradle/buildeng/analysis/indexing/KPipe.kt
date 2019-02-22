package org.gradle.buildeng.analysis.indexing

import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.api.services.bigquery.model.TimePartitioning
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.coders.NullableCoder
import org.apache.beam.sdk.io.FileIO
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder
import org.apache.beam.sdk.io.gcp.bigquery.WriteResult
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.Filter
import org.apache.beam.sdk.transforms.FlatMapElements
import org.apache.beam.sdk.transforms.MapElements
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.values.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object KPipe {
    inline fun <reified R : PipelineOptions> from(args: Array<String>): Pair<Pipeline, R> {
        val options = PipelineOptionsFactory.fromArgs(*args)
                .withValidation()
                .`as`(R::class.java)
        return Pipeline.create(options) to options
    }
}

fun Pipeline.fromText(
        name: String = "Read from Text",
        path: String): PCollection<String> {
    return this.apply(name,
            TextIO.read().from(path))
}

fun Pipeline.fromFiles(
        name: String = "Read from File Collection",
        input: String): PCollection<KV<String, String>> {

    return this.apply(name, FileIO.match().filepattern(input))
            .apply("$name readMatches", FileIO.readMatches())
            .apply("$name read files", MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings())).via(SerializableFunction { file: FileIO.ReadableFile ->
                KV.of(file.metadata.resourceId().toString(), file.readFullyAsUTF8String())
            }))
}

fun PCollection<String>.toText(
        name: String = "Write to Text",
        filename: String
): PDone {
    return this.apply(name,
            TextIO.write().to(filename))
}

fun PCollection<TableRow>.toTable(
        name: String = "Write to BigQuery table",
        tableId: String,
        tableSchema: TableSchema,
        timePartitioning: TimePartitioning = TimePartitioning()
): WriteResult {
    return this.apply(name,
            BigQueryIO.writeTableRows()
                    .withExtendedErrorInfo()
                    .withSchema(tableSchema)
                    .to(tableId)
                    .withTimePartitioning(timePartitioning)
                    .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                    .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND))
}

inline fun <I, reified O> PCollection<I>.map(
        name: String = "map to ${O::class.simpleName}",
        noinline transform: (I) -> O): PCollection<O> {
    val pc = this.apply(name,
            MapElements.into(TypeDescriptor.of(O::class.java))
                    .via(transform))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

inline fun <reified I> PCollection<I>.filter(
        name: String = "filter ${I::class.simpleName}",
        noinline transform: (I) -> Boolean): PCollection<I> {
    val pc = this.apply(name, Filter.by(SerializableFunction { transform(it) }))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

inline fun <I, reified O> PCollection<I>.flatMap(
        name: String = "flatMap to ${O::class.simpleName}",
        noinline transform: (I) -> Iterable<O>): PCollection<O> {
    val pc = this.apply(name, FlatMapElements.into(TypeDescriptor.of(O::class.java))
            .via(transform))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

/**
 * Converts a JSON string to a [TableRow] object. If the data fails to convert, a RuntimeException will be thrown.
 *
 * @param json The JSON string to parse.
 * @return The parsed [TableRow] object.
 */
fun convertJsonToTableRow(json: String): TableRow? {
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
