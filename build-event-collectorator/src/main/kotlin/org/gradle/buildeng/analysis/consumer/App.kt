package org.gradle.buildeng.analysis.consumer

import org.gradle.buildeng.analysis.common.ServerConnectionInfo

fun main(args: Array<String>) {
    BuildConsumer(ServerConnectionInfo.fromEnv())
            .consume(
                System.getenv("PUBSUB_SUBSCRIPTION_ID"),
                System.getenv("GCS_RAW_BUCKET_NAME"))
}
