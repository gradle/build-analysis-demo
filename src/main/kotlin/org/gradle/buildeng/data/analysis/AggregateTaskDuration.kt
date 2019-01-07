package org.gradle.buildeng.data.analysis

import org.gradle.buildeng.data.model.TaskExecution
import org.nield.kotlinstatistics.geometricMean
import org.nield.kotlinstatistics.standardDeviation

class AggregateTaskDuration {
    data class TaskCost(val path: String, val meanWallClockTime: Double, val wallClockStdDev: Double, val frequency: Int) {
        private fun fmt(input: Double) = "%.2f".format(input)

        fun asTsv() = "$path\t$frequency\t${fmt(meanWallClockTime)}\t${fmt(wallClockStdDev)}"
    }

    fun calculateTaskCosts(taskExecutions: Collection<TaskExecution>): Collection<TaskCost> {
        return taskExecutions
                .groupBy { it.path }
                .mapValues { entry ->
                    val wallClockTimes = entry.value.map { it.executionDuration.toMillis() }
                    TaskCost(entry.key, wallClockTimes.geometricMean(), wallClockTimes.standardDeviation(), entry.value.size)
                }
                .values
    }
}
