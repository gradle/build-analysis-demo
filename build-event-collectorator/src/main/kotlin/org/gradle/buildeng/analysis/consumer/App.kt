package org.gradle.buildeng.analysis.consumer

import org.gradle.buildeng.analysis.common.ServerConnectionInfo

fun main() {
    val startTimestamp = System.getenv("START_TIMESTAMP").toLong()

    BuildConsumer(ServerConnectionInfo.fromEnv())
            .consume(startTimestamp, System.getenv("GCS_RAW_BUCKET_NAME"))
}
