package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.*
import org.gradle.buildeng.analysis.model.BuildEvent
import java.time.Instant

class DependencyResolutionEventsJsonTransformer : EventsJsonTransformer() {
    fun transform(fileContents: String): String {
        val list = fileContents.split("\n")
        if (list.isEmpty()) {
            throw IllegalArgumentException("Cannot transform empty input")
        }

        // Read first line, then everything else is events
        val header = objectReader.readTree(list.first())

        var rootProjectName = "UNKNOWN_ROOT_PROJECT"
        val buildId = header.get("buildId").asText()
        val timestamp = Instant.ofEpochMilli(header.get("timestamp").asLong())
        val moduleDependencies = mutableListOf<ModuleDependency>()
        val projectDependencies = mutableListOf<ProjectDependency>()
        val unknownTypeDependencies = mutableListOf<UnknownTypeDependency>()
        val repositories = mutableListOf<Repository>()
        val failureIds = mutableListOf<String>()
        var failures: String? = null

        val rawBuildEvents = list.drop(1)

        rawBuildEvents.filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            when (buildEvent?.type?.eventType) {
                "ProjectStructure" -> {
                    // This event is triggered for every included build, so is the only way to get root project
                    if (buildEvent.data.path("projects").any { project -> project.path("buildPath").asText() == ":" }) {
                        rootProjectName = buildEvent.data.get("rootProjectName").asText()
                    }
                }
                "ConfigurationResolutionData" -> {
                    // TODO: look up component matching identity ID and get repository ID from that
                    failureIds.addAll(buildEvent.data.path("failureIds").map { f -> f.asText() })
                    failures = buildEvent.data.path("failures").asText()

                    val identities = buildEvent.data.get("identities")
                    val resolvedDependencyIds = buildEvent.data.get("dependencies").fields().asSequence().map { dependency -> dependency.value.get("to").asText() }
                    resolvedDependencyIds.forEach { id ->
                        val node: JsonNode? = identities.get(id)
                        if (node == null) {
                            println("WARNING: Unable to get component identity for resolved dependency id [$id]")
                        } else {
                            when (node.path("type").asText()) {
                                "ModuleComponentIdentity_1_0" -> moduleDependencies.add(ModuleDependency(node.path("group").asText(), node.path("module").asText(), node.path("version").asText()))
                                "ProjectComponentIdentity_1_0" -> projectDependencies.add(ProjectDependency(null, node.path("projectPath").asText()))
                                "ProjectComponentIdentity_1_1" -> projectDependencies.add(ProjectDependency(node.path("buildPath").asText(), node.path("projectPath").asText()))
                                "UnknownTypeComponentIdentity_1_0" -> unknownTypeDependencies.add(UnknownTypeDependency(node.path("className").asText(), node.path("displayName").asText()))
                                else -> println("WARNING: Unknown dependency type [$it] encountered in build $buildId")
                            }
                        }
                    }
                }
                "Repository" -> repositories.add(
                        Repository(
                                buildEvent.data.get("id").asText(),
                                buildEvent.data.path("name").asText(),
                                buildEvent.data.path("type").asText(),
                                objectWriter.writeValueAsString(buildEvent.data.path("properties"))))

            }
        }

        val dependencyResolution = DependencyResolution(rootProjectName, buildId, timestamp, moduleDependencies, projectDependencies, unknownTypeDependencies, repositories, failureIds, failures)
        return objectWriter.writeValueAsString(objectMapper.convertValue(dependencyResolution, JsonNode::class.java))
    }
}
