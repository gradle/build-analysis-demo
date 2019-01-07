package org.gradle.buildeng.data.analysis

import org.gradle.buildeng.data.model.ExportBuild


/**
 * Given a collection of Builds calculate the feedback loop time by build agent (CI instance or human)
 */
class FeedbackLoopAnalysis {

    data class FeedbackLoop(val agent: String, val buildDurationMillis: Int, val loopDurationMillis: Int, val quintile: IntRange)


    fun calculateFeedbackLoops(builds: Collection<ExportBuild>, quintiles: Collection<IntRange>): Collection<FeedbackLoop> {
        if (builds.isEmpty()) return emptyList()

        val groupedByAgent: Map<String, List<ExportBuild>> = builds.sortedBy { it.startDateTime }.groupBy { it.agent }

        val feedbackLoops = mutableListOf<FeedbackLoop>()

        groupedByAgent.mapValues {
            val iterator = it.value.iterator()
            var prevBuild: ExportBuild = iterator.next()
            var curBuild: ExportBuild?
            while (iterator.hasNext()) {
                // TODO: ensure it's the same project and less than an hour between build end=>start
                curBuild = iterator.next()
                val quintile = quintiles.first { it.contains(prevBuild.durationMillis) }
                val feedbackLoopDuration: Int = (curBuild.startDateTime.toEpochMilli() - prevBuild.startDateTime.toEpochMilli()).toInt()
                feedbackLoops.add(0, FeedbackLoop(prevBuild.agent, prevBuild.durationMillis, feedbackLoopDuration, quintile))
                prevBuild = curBuild
            }
        }

        return feedbackLoops
    }
}
