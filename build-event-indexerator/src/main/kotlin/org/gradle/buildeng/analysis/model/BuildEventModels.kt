package org.gradle.buildeng.analysis.model

import java.time.Duration
import java.time.Instant

data class Build(
        val buildId: String,
        val rootProjectName: String,
        val buildTool: String,
        val buildToolVersion: String,
        val buildAgentId: String,
        val buildRequestedTasks: List<String>,
        val buildExcludedTasks: List<String>,
        val environmentParameters: List<EnvironmentParameter>,
        val buildTimestamp: Instant,
        val wallClockDuration: Duration,
        val failureIds: List<String>,
        val failed: Boolean,
        val failureData: FailureData?,
        val userLink: List<UserLink>,
        val userNamedValue: List<UserNamedValue>,
        val userTag: List<String>
)

data class EnvironmentParameter(val key: String, val value: String)
data class UserLink(val label: String, val url: String)
data class UserNamedValue(val key: String, val value: String)

data class TaskOrGoalExecution(
        val id: String,
        val name: String,
        val className: String,
        var failureId: String? = null
)

data class FailureData(
        val category: String,
        val causes: List<ExceptionData>
)

data class ExceptionData(
        val exceptionId: String,
        val exceptionClassName: String,
        val message: String,
        val failedTaskGoalName: String?,
        val failedTaskTypeOrMojoClassName: String?
)
