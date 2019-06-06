package org.gradle.buildeng.analysis.transform

import org.gradle.buildeng.analysis.model.ExceptionData
import kotlin.test.Test
import kotlin.test.assertEquals

class FailureClassifierTest {
    @Test
    fun testClassifyTestTaskFailed() {
        val input = listOf(
                ExceptionData("EXCEPTION_ID", "org.gradle.api.tasks.TaskExecutionException", "Execution failed for task ':performance:distributedPerformanceTest'.", ":performance:distributedPerformanceTest", "org.gradle.testing.RerunableDistributedPerformanceTest"),
                exceptionData("org.gradle.process.internal.ExecException", "Process 'command '/opt/files/jdk-linux/OpenJDK11U-jdk_x64_linux_hotspot_11.0.3_7.tar.gz/bin/java'' finished with non-zero exit value 1"),
                exceptionData("org.gradle.internal.exceptions.LocationAwareException", "Execution failed for task ':performance:distributedPerformanceTest'.")
        )

        assertEquals(FailureClassification.VERIFICATION, FailureClassifier.classifyFailure(input))
    }

    @Test
    fun testClassifyMultipleFailures() {
        val input = listOf(
                ExceptionData("40691557953630312", "org.gradle.api.tasks.TaskExecutionException", "Execution failed for task ':ear:integTest'.", ":ear:integTest", "org.gradle.gradlebuild.test.integrationtests.IntegrationTest"),
                exceptionData("java.lang.IllegalStateException", "Too many failures (42) in first run!"),
                exceptionData("org.gradle.execution.MultipleBuildFailures", "Build completed with 2 failures."),
                ExceptionData("-5538819697401811179", "org.gradle.internal.exceptions.LocationAwareException", "Execution failed for task ':ear:test'.", ":ear:test", "org.gradle.api.tasks.testing.Test")
        )

        assertEquals(FailureClassification.VERIFICATION, FailureClassifier.classifyFailure(input))
    }

    @Test
    fun testClassifySpockFailure() {
        val input = listOf(
                exceptionData("org.gradle.internal.dispatch.DispatchException", "Could not dispatch message [MethodInvocation method: stop()]."),
                exceptionData("org.gradle.integtests.fixtures.executer.UnexpectedBuildFailure", "Gradle execution failed in /home/tcagent1/agent/work/668602365d1521fc/subprojects/plugin-use/build/tmp/test"),
                exceptionData("org.spockframework.runtime.ConditionFailedWithExceptionError", "Condition failed with Exception:"),
                ExceptionData("-4618799316195560121", "org.gradle.api.tasks.TaskExecutionException", "Execution failed for task ':pluginUse:forkingIntegTest'.", ":pluginUse:forkingIntegTest", "org.gradle.gradlebuild.test.integrationtests.IntegrationTest")
        )

        assertEquals(FailureClassification.VERIFICATION, FailureClassifier.classifyFailure(input))
    }

    private fun exceptionData(className: String, message: String): ExceptionData {
        return ExceptionData("EXCEPTION_ID", className, message, null, null)
    }
}
