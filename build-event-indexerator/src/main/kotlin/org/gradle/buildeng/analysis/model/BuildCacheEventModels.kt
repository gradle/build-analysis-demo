package org.gradle.buildeng.analysis.model

import java.time.Duration
import java.time.Instant

data class BuildCacheInteraction(
        val id: String,
        val buildId: String,
        val taskId: String,
        val type: String,
        val cacheKey: String,
        val startTimestamp: Instant,
        var duration: Duration? = null,
        var failureId: String? = null
)
