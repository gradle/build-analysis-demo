package org.gradle.buildeng.analysis.model

import java.time.Duration
import java.time.Instant

data class TasksContainer(
        val rootProjectName: String,
        val tasks: List<Task>
)

data class Task(
        val buildPath: String,
        val path: String,
        val className: String,
        val executions: List<TaskExecution>
)

data class TaskExecution(
        val buildId: String,
        val buildAgentId: String,
        val startTimestamp: Instant,
        val wallClockDuration: Duration,
        val outcome: String
)
