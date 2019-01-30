package org.gradle.buildeng.analysis.model

import java.time.Duration
import java.time.Instant

/**
 * -- Did dependency resolution from Bintray get more reliable after Gradle 5.0? Did it get slower or faster?
 *
 * select _PARTITIONDAY as day, hostname, MEAN(duration) as mean_duration, STDDEV(duration) as std_dev, count(id)
 * from network_activity
 * where _PARTITIONDAY in last 7 days
 *  and hostname like '%bintray.com%'
 * group by 1, 2, 3, 4
 */

data class NetworkActivity(
        val buildId: String,
        val id: String,
        val hostname: String?,
        val path: String,
        val contentLength: Long,
        val startTimestamp: Instant,
        val duration: Duration? = null,
        val failureId: String? = null,
        val failure: String? = null)
