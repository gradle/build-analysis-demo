package org.gradle.buildeng.analysis.indexing

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.NullableCoder
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.FlatMapElements
import org.apache.beam.sdk.transforms.MapElements
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PDone
import org.apache.beam.sdk.values.TypeDescriptor

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

fun PCollection<String>.toText(
        name: String = "Write to Text",
        filename: String
): PDone {
    return this.apply(name,
            TextIO.write().to(filename))
}

//fun PCollection<String>.toTable(
//        name: String = "Write to BigQuery table",
//        tableName: String
//): PDone {
//    return this.apply(name,
//            BigQueryIO.write().to(tableName))
//}


inline fun <I, reified O> PCollection<I>.map(
        name: String = "map to ${O::class.simpleName}",
        noinline transform: (I) -> O): PCollection<O> {
    val pc = this.apply(name,
            MapElements.into(TypeDescriptor.of(O::class.java))
                    .via(transform))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

inline fun <I, reified O> PCollection<I>.flatMap(
        name: String = "flatMap to ${O::class.simpleName}",
        noinline transform: (I) -> Iterable<O>): PCollection<O> {
    val pc = this.apply(name, FlatMapElements.into(TypeDescriptor.of(O::class.java))
            .via(transform))
    return pc.setCoder(NullableCoder.of(pc.coder))
}
