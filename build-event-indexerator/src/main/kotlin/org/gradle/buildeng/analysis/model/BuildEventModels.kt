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
        val failureId: String?,
        val failed: Boolean,
        val userLink: List<UserLink>,
        val userNamedValue: List<UserNamedValue>,
        val userTag: List<String>
)

data class EnvironmentParameter(val key: String, val value: String)
data class UserLink(val label: String, val url: String)
data class UserNamedValue(val key: String, val value: String)
