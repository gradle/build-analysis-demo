package org.gradle.buildeng.analysis.model

import java.time.Instant

data class DependencyResolution(
        val rootProjectName: String,
        val buildId: String,
        val buildTimestamp: Instant,
        val moduleDependencies: List<ModuleDependency>,
        val projectDependencies: List<ProjectDependency>,
        val unknownTypeDependencies: List<UnknownTypeDependency>,
        val repositories: List<Repository>,
        val failureIds: List<String>,
        val failures: String?
)

data class ModuleDependency(val group: String?, val module: String?, val version: String?)

data class ProjectDependency(val buildPath: String?, val projectPath: String)

data class UnknownTypeDependency(val className: String?, val displayName: String?)

data class Repository(val id: String, val name: String, val type: String, val properties: String?)
