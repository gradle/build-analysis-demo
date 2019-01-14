package org.gradle.buildeng.analysis.consumer

import org.gradle.buildeng.analysis.common.ServerConnectionInfo
import java.time.Duration
import java.time.Instant

fun main(args: Array<String>) {
    val since = Instant.now().minus(
            Duration.ofDays(System.getenv("BACKFILL_DAYS").toLong())
    )
    BuildConsumer(ServerConnectionInfo.fromEnv())
            .consume(since, System.getenv("LAST_BUILD_ID"), System.getenv("GCS_RAW_BUCKET_NAME"))
}
