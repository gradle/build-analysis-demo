package org.gradle.buildeng.analysis.transform

fun main(args: Array<String>) {
    BuildTransformer().transform(
            BigQueryBuildEventsJsonTransformer(),
            System.getenv("GCS_RAW_BUCKET_NAME"),
            System.getenv("GCS_TRANSFORMED_BUCKET_NAME"))
}
