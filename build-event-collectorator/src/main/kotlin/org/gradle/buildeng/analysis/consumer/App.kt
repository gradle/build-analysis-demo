package org.gradle.buildeng.analysis.consumer

import org.gradle.buildeng.analysis.common.ServerConnectionInfo

fun main() {
    val startTimestamp = System.getenv("START_TIMESTAMP").toLong()
    val stopTimestamp = System.getenv("STOP_TIMESTAMP").toLong()

    BuildConsumer(ServerConnectionInfo.fromEnv())
            .consume(startTimestamp, stopTimestamp, System.getenv("GCS_RAW_BUCKET_NAME"))
}
