package org.gradle.buildeng.analysis.producer

import org.gradle.buildeng.analysis.common.ServerConnectionInfo
import java.time.Duration
import java.time.Instant

fun main(args: Array<String>) {
    // TODO: change this to days
    val since = Instant.now().minus(Duration.ofMinutes(5))
    BuildProducer(ServerConnectionInfo.fromEnv())
            .produceFrom(
                    System.getenv("PUBSUB_TOPIC"),
                    since)
}
