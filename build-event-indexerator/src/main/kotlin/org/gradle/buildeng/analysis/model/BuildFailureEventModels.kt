package org.gradle.buildeng.analysis.model

import java.time.Instant

data class BuildFailure(
        val buildId: String,
        var rootProjectName: String = "UNKNOWN_PROJECT",
        var buildAgentId: String = "UNKNOWN_BUILD_AGENT",
        val timestamp: Instant,
        val exceptions: List<Failure>
)

data class Failure(
        val exceptionId: String,
        val taskId: String?,
        val className: String,
        val message: String,
        val causes: List<String>
)
