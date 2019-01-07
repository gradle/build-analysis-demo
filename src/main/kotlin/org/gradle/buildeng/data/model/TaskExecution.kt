package org.gradle.buildeng.data.model

import java.time.Duration
import java.time.Instant

data class TaskExecution(
        val buildId: String,
        val taskStartDate: Instant,
        val rootProjectName: String,
        val executionDuration: Duration,
        val path: String,
        val type: String,
        val outcome: String,
        val isCI: Boolean)
