package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.JsonNode
import org.gradle.buildeng.analysis.model.BuildEvent
import org.gradle.buildeng.analysis.model.NetworkActivity
import java.net.URL
import java.time.Duration

class NetworkActivityEventsJsonTransformer : EventsJsonTransformer() {

    fun transform(fileContents: String): List<String> {
        val list = fileContents.split("\n")
        // Read first line, then everything else is events
        val header = objectReader.readTree(list.first())
        val buildId = header.get("buildId").asText()

        val networkActivities = mutableMapOf<String, NetworkActivity>()
        list.drop(1).filter { it.isNotEmpty() }.forEach {
            val buildEvent = BuildEvent.fromJson(objectReader.readTree(it))
            when (buildEvent?.type?.eventType) {
                "NetworkDownloadActivityStarted" -> {
                    val id = buildEvent.data.get("id").asText()
                    try {
                        val url = URL(buildEvent.data.path("location").asText())
                        val contentLength = buildEvent.data.get("contentLength").asLong()
                        networkActivities[id] = NetworkActivity(buildId, id, url.host, url.path, contentLength, buildEvent.timestamp)
                    } catch (e: Exception) {
                        println("Got exception parsing URL: ${e.message}")
                    }
                }
                "NetworkDownloadActivityFinished" -> {
                    val id = buildEvent.data.get("id").asText()
                    try {
                        val startedActivity = networkActivities[id]!!
                        networkActivities[id] = startedActivity.copy(
                                duration = Duration.between(startedActivity.startTimestamp, buildEvent.timestamp),
                                failureId = buildEvent.data.path("failureId").asText(),
                                failure = buildEvent.data.path("failure").asText()
                        )
                    } catch (e: Exception) {
                        println("Network Activity Started with ID [$id] could not be found")
                    }
                }
            }
        }

        return networkActivities.map {
            objectWriter.writeValueAsString(objectMapper.convertValue(it.value, JsonNode::class.java))
        }
    }
}
