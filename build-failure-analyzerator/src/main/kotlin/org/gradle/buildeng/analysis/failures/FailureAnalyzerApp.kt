package org.gradle.buildeng.analysis.failures

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

data class BuildData(
        val buildId: String,
        val project: String,
        val buildRequestedTasks: List<String>,
        val buildTimestamp: String, // TODO: Instant, if we care
        val buildAgentId: String,
        val os: String,
        val failureData: FailureData
)

data class FailureData(
        val category: String,
        val taskPaths: List<String>,
        val causes: List<ExceptionData>
)

data class ExceptionData(
        val exceptionId: String,
        val className: String,
        val message: String
)

object VectorSimilarity {
    fun setSimilarity(first: Set<Long>, second: Set<Long>, threshold: Double): Boolean {
        val intersection = first.intersect(second)
        return intersection.size / first.size.toDouble() >= threshold && intersection.size / second.size.toDouble() >= threshold
    }
}

data class BuildVector(
        val projectHash: Long,
        val requestedTasksHash: Set<Long>,
        val osHash: Long,
        val failureVector: FailureVector
) {
    // Return the likelihood that the given build vector is related to this one between 0 and 1
    fun similarityTo(other: BuildVector): Double {
        var similarity = 0.75 * failureVector.similarityTo(other.failureVector)

        if (this.projectHash == other.projectHash) {
            similarity += 0.05
        }
        if (this.osHash == other.osHash) {
            similarity += 0.05
        }
        if (VectorSimilarity.setSimilarity(requestedTasksHash, other.requestedTasksHash, 0.9)) {
            similarity += 0.15
        }

        return similarity
    }
}

data class FailureVector(
        // FIXME(EW): Seems like we're losing information here... I think each failure cause should have a related task, right?
        val failedTaskPathsHash: Set<Long>,
        val failureCauseVectors: List<FailureCauseVector>
) {
    fun similarityTo(other: FailureVector): Double {
        var similarity = 0.0
        if (VectorSimilarity.setSimilarity(failedTaskPathsHash, other.failedTaskPathsHash, 0.9)) {
            similarity += 0.3
        }

        failureCauseVectors.sumBy { vector ->
            var highestSimilarity = 0.0
            other.failureCauseVectors.forEach {
                val sim = vector.similarityTo(it)
                if (sim > highestSimilarity) {
                    highestSimilarity = sim
                }
            }
            return highestSimilarity
        } / failureCauseVectors.size * 0.7

        return similarity
    }
}

data class FailureCauseVector(
        val messageVectors: Set<Long>,
        val failureClassNameHash: Long
) {
    fun similarityTo(other: FailureCauseVector): Double {
        var similarity = 0.0
        if (failureClassNameHash == other.failureClassNameHash) {
            similarity += 0.3
        }

        val intersection = messageVectors.intersect(other.messageVectors).size

        similarity += (0.35 * intersection / messageVectors.size)
        similarity += (0.35 * intersection / other.messageVectors.size)

        return similarity
    }
}

object FailureAnalyzerApp {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {
        val failureJson4 = "{\"buildId\":\"tzj4qelj5kyxs\",\"project\":\"dotcom\",\"buildRequestedTasks\":[\"clean\",\":enterprise:releaseAndPromoteShipUnstableRelease\"],\"buildTimestamp\":\"2019-05-03 07:22:11.603 UTC\",\"buildAgentId\":\"tcagent1@dev24.gradle.org\",\"os\":\"\\\"Linux\\\"\",\"failureData\":{\"category\":\"UNCLASSIFIED\",\"causes\":[{\"className\":\"java.lang.IllegalStateException\",\"exceptionId\":\"-2450571649525542303\",\"message\":\"Error while promoting the release [One or more linter errors: Missing required property: items [config.v1.6],Missing required property: items [config.v1.7],Missing required property: items [config.v1.8],Missing required property: items [config.v1.9],Missing required property: items [config.v1.10],Missing required property: items [config.v1.11],Unknown property (not in schema) [config.v1.6.help_text],Unknown property (not in schema) [config.v1.6.type],Unknown property (not in schema) [config.v1.7.type],Unknown property (not in schema) [config.v1.7.default],Unknown property (not in schema) [config.v1.7.help_text],Unknown property (not in schema) [config.v1.7.test_proc],Unknown property (not in schema) [config.v1.8.help_text],Unknown property (not in schema) [config.v1.8.type],Unknown property (not in schema) [config.v1.9.type],Unknown property (not in schema) [config.v1.9.default],Unknown property (not in schema) [config.v1.9.help_text],Unknown property (not in schema) [config.v1.9.test_proc],Unknown property (not in schema) [config.v1.10.help_text],Unknown property (not in schema) [config.v1.10.type],Unknown property (not in schema) [config.v1.11.type],Unknown property (not in schema) [config.v1.11.default],Unknown property (not in schema) [config.v1.11.help_text],Unknown property (not in schema) [config.v1.11.test_proc]]\"}],\"taskPaths\":[\":enterprise:releaseAndPromoteShipUnstableRelease\"]}}"

        val failureJson1208 = "{\"buildId\":\"2fdppgcxtneak\",\"project\":\"gradle\",\"buildRequestedTasks\":[\"clean\",\"largeJavaMultiProject\",\"fullPerformanceTests\",\"--scenarios\",\"cold daemon on largeJavaMultiProject\",\"--baselines\",\"defaults\",\"--warmups\",\"defaults\",\"--runs\",\"defaults\",\"--checks\",\"all\",\"--channel\",\"experiments-master\"],\"buildTimestamp\":\"2019-05-13 01:55:48.774 UTC\",\"buildAgentId\":\"tcagent1@dev28.gradle.org\",\"os\":\"\\\"Linux\\\"\",\"failureData\":{\"category\":\"VERIFICATION\",\"causes\":[{\"className\":\"java.lang.AssertionError\",\"exceptionId\":\"-3545824737242219\",\"message\":\"Speed Results for test project 'largeJavaMultiProject' with tasks tasks: we're slower than 5.4-20190314000100+0000 with 99% confidence.\\nDifference: 74.5 ms slower (74.5 ms), 0.84%\\n  Current Gradle median: 8.978 s min: 8.808 s, max: 9.154 s, se: 81.42 ms}\\n  \\u003e [9.037 s, 8.98 s, 8.938 s, 8.891 s, 8.977 s, 8.921 s, 8.915 s, 9.154 s, 8.94 s, 8.924 s, 8.819 s, 9.037 s, 9.14 s, 9.146 s, 8.934 s, 8.845 s, 8.897 s, 9.021 s, 9.05 s, 8.808 s, 9.032 s, 8.994 s, 8.987 s, 9.026 s, 9.022 s, 8.968 s, 9.026 s, 8.958 s, 8.985 s, 9.018 s, 9.07 s, 9.008 s, 8.858 s, 8.954 s, 8.891 s, 8.917 s, 8.902 s, 9.054 s, 8.907 s, 9.011 s]\\n  Gradle 5.4-20190314000100+0000 median: 8.904 s min: 8.704 s, max: 9.234 s, se: 107.07 ms}\\n  \\u003e [9.234 s, 8.996 s, 8.895 s, 8.961 s, 8.991 s, 8.894 s, 8.919 s, 8.957 s, 9.081 s, 8.882 s, 8.858 s, 9.036 s, 8.886 s, 8.861 s, 8.836 s, 8.986 s, 8.876 s, 9.076 s, 8.712 s, 8.907 s, 9.112 s, 8.826 s, 8.772 s, 8.91 s, 8.792 s, 9.019 s, 8.994 s, 8.911 s, 8.954 s, 8.87 s, 8.781 s, 9.064 s, 8.861 s, 8.901 s, 8.858 s, 8.806 s, 8.946 s, 8.704 s, 8.88 s, 8.96 s]\\n\\n\"},{\"className\":\"org.gradle.api.GradleException\",\"exceptionId\":\"-8558071398017843781\",\"message\":\"There were failing tests. See the report at: file:///home/tcagent1/agent/work/668602365d1521fc/subprojects/performance/build/reports/tests/fullPerformanceTest/index.html\"}],\"taskPaths\":[\":performance:fullPerformanceTest\"]}}"

        val failureJson76 = "{\"buildId\":\"ofl3fxo4hj2hu\",\"project\":\"gradle\",\"buildRequestedTasks\":[\"clean\",\"sanityCheck\"],\"buildTimestamp\":\"2019-05-13 11:26:11.964 UTC\",\"buildAgentId\":\"tcagent1@dev107.gradle.org\",\"os\":\"\\\"Linux\\\"\",\"failureData\":{\"category\":\"BUILD_ENVIRONMENT\",\"causes\":[{\"className\":\"org.apache.tools.ant.BuildException\",\"exceptionId\":\"1671936032257641754\",\"message\":\"Exceeded maximum number of priority 3 violations: (p1=0; p2=0; p3=1)\"},{\"className\":\"javax.net.ssl.SSLPeerUnverifiedException\",\"exceptionId\":\"8907647335049403681\",\"message\":\"peer not authenticated\"},{\"className\":\"org.gradle.process.internal.ExecException\",\"exceptionId\":\"-1034395638880745043\",\"message\":\"Process 'command '/opt/files/jdk-linux/openjdk-11.0.2_linux-x64_bin.tar.gz/bin/java'' finished with non-zero exit value 1\"}],\"taskPaths\":[\":instantExecution:ktlintMainSourceSetCheck\",\":core:codenarcTest\"]}}"

        val failureJson = "{\"buildId\":\"gwmuwnujhu2ek\",\"project\":\"dotcom\",\"buildRequestedTasks\":[\"clean\",\":enterprise-test-connectivity:enterpriseShipUnstableReleaseReplicatedTest\",\"--tests\",\"*LicenseDrivenFeaturesConnectivityTest\"],\"buildTimestamp\":\"2019-05-03 12:05:43.161 UTC\",\"buildAgentId\":\"tcagent1@dev23.gradle.org\",\"os\":\"\\\"Linux\\\"\",\"failureData\":{\"category\":\"UNCLASSIFIED\",\"causes\":[{\"className\":\"org.gradle.api.GradleException\",\"exceptionId\":\"-5511296647420846718\",\"message\":\"Failed to execute the Vagrant command.\"}],\"taskPaths\":[\":enterprise-test-connectivity:startShipUnstableReleaseVagrant\"]}}"

        val targetFailure = objectMapper.readValue<BuildData>(failureJson)
        val targetVector = vectorize(targetFailure)

        val dataFile = File(this::class.java.classLoader.getResource("gradle-failure-data.json").file)
        dataFile.useLines { list ->
            val failedBuilds = list.filter { it.isNotEmpty() }.map {
                objectMapper.readValue<BuildData>(it)
            }
            val matches = failedBuilds.filter { build ->
                val currentVector = vectorize(build)
                val b = targetVector.similarityTo(currentVector) >= 0.7 && currentVector.similarityTo(targetVector) >= 0.7
                if (b) {
                    println("Found match with scores [${targetVector.similarityTo(currentVector)}] and [${currentVector.similarityTo(targetVector)}]")
                }
                b
            }.toList().size

            println(matches)
        }
    }

    private fun vectorize(failedBuild: BuildData): BuildVector {
        val failureCauseVectors = failedBuild.failureData.causes.map { cause ->
            FailureCauseVector(messageSummary(cause.message).split(' ').map { hash(it) }.toSet(), hash(cause.className))
        }
        val failureVector = FailureVector(failedBuild.failureData.taskPaths.map { hash(it) }.toSet(), failureCauseVectors)

        return BuildVector(hash(failedBuild.project), failedBuild.buildRequestedTasks.map { hash(it) }.toSet(), hash(failedBuild.os), failureVector)
    }

    @JvmStatic
    fun messageSummary(message: String): String = message.trimStart().substringBefore("\n").take(255)

    @JvmStatic
    fun hash(string: String): Long {
        // adapted from String.hashCode()
        var h = 1125899906842597L // prime
        val len = string.length

        for (i in 0 until len) {
            h = 31 * h + string[i].toLong()
        }
        return h
    }
}
