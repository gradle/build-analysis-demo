package org.gradle.buildeng.analysis.model

import java.time.Duration
import java.time.Instant

data class TaskExecutions(
        val buildId: String,
        val buildTimestamp: Instant,
        var rootProjectName: String = "UNKNOWN_PROJECT",
        var buildAgentId: String = "UNKNOWN_BUILD_AGENT",
        val tasks: List<TaskExecution>
)

data class TaskExecution(
        val taskId: String,
        val path: String,
        val startTimestamp: Instant,
        var wallClockDuration: Duration? = null,
        var avoidedExecutionTimeMs: Long? = null,
        var buildPath: String? = null,
        var className: String? = null,
        var outcome: String? = null,
        var cacheable: Boolean? = null,
        var cachingDisabledReasonCategory: String? = null,
        var actionable: Boolean? = null,
        val buildCacheInteractionIds: MutableList<String>
)
