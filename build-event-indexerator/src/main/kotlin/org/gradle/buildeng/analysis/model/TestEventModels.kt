package org.gradle.buildeng.analysis.model

import java.time.Duration
import java.time.Instant

data class TestsContainer(
        val rootProjectName: String,
        val buildId: String,
        val buildTimestamp: Instant,
        val buildAgentId: String,
        val tests: List<Test>
)

data class Test(
        val suite: Boolean,
        val className: String?,
        val name: String,
        val taskId: String,
        val executions: List<TestExecution>
)

data class TestExecution(
        val startTimestamp: Instant,
        val wallClockDuration: Duration,
        val skipped: Boolean,
        val failed: Boolean,
        val failureId: String?,
        val failure: String?
)
