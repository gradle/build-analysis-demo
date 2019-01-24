package org.gradle.buildeng.analysis

import org.gradle.buildeng.analysis.indexing.RawBuildJsonIndexer
import org.gradle.buildeng.analysis.transform.BigQueryBuildEventsJsonTransformer
import org.gradle.buildeng.analysis.transform.BuildTransformer

fun main(args: Array<String>) {
    BuildTransformer().transform(
            BigQueryBuildEventsJsonTransformer(),
            System.getenv("GCS_RAW_BUCKET_NAME"),
            System.getenv("GCS_TRANSFORMED_BUCKET_NAME"))

    val sourceLocation = "gs://${System.getenv("GCS_TRANSFORMED_BUCKET_NAME")}/*"
    RawBuildJsonIndexer.index(
            System.getenv("BIGQUERY_DATASET_NAME"),
            System.getenv("BIGQUERY_TABLE_NAME"),
            sourceLocation)
}
