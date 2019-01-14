package org.gradle.buildeng.analysis.producer

import org.gradle.buildeng.analysis.common.ServerConnectionInfo
import java.time.Duration
import java.time.Instant

fun main(args: Array<String>) {
    val since = Instant.now().minus(
            Duration.ofDays(System.getenv("BACKFILL_DAYS").toLong())
    )
    BuildProducer(ServerConnectionInfo.fromEnv())
            .produceFrom(System.getenv("PUBSUB_TOPIC"), since, System.getenv("LAST_BUILD_ID"))
}
