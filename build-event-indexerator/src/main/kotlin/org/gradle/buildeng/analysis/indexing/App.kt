package org.gradle.buildeng.analysis.indexing

fun main(args: Array<String>) {
    val sourceLocation = "gs://${System.getenv("GCS_TRANSFORMED_BUCKET_NAME")}/*"
    BuildIndexer().index(
            System.getenv("BIGQUERY_DATASET_NAME"),
            System.getenv("BIGQUERY_TABLE_NAME"),
            sourceLocation)
}
