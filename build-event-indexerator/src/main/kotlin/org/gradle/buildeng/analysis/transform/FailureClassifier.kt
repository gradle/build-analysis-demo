package org.gradle.buildeng.analysis.transform

import org.gradle.buildeng.analysis.model.ExceptionData

enum class FailureClassification {
    VERIFICATION,
    BUILD_CONFIGURATION,
    BUILD_ENVIRONMENT,
    UNCLASSIFIED
}

interface BinaryFailureClassifier {
    /**
     * Given Failure information for a single build, return the likelihood that the
     * build failure should be categorized as the given type of failure or not.
     * The probability is represented as a Double between 0 and 1, inclusive.
     */
    fun classify(exceptions: List<ExceptionData>): Double
}

/**
 * Classify a failure as one caused by the environment: including hardware, external services
 */
object InfrastructureFailureClassifier : BinaryFailureClassifier {
    private val knownSystemErrorRelatedExceptionClassNames = listOf("java.lang.OutOfMemoryError", "java.lang.StackOverflowError", "java.lang.SSLException", "org.gradle.internal.resource.transport.http.HttpErrorStatusCodeException", "org.gradle.api.file.UnableToDeleteFileException", "java.net.SocketTimeoutException", "javax.net.ssl.SSLPeerUnverifiedException", "org.h2.jdbc.JdbcSQLException", "java.net.SocketException", "org.postgresql.util.PSQLException", "org.openqa.selenium.NoSuchSessionException", "org.openqa.selenium.WebDriverException", "org.apache.http.NoHttpResponseException")

    override fun classify(exceptions: List<ExceptionData>): Double {
        if (exceptions.any { knownSystemErrorRelatedExceptionClassNames.contains(it.exceptionClassName) }) {
            if (exceptions.size > 1) {
                return 0.99
            }
            return 0.95
        }
        return 0.05
    }
}

object VerificationFailureClassifier : BinaryFailureClassifier {
    override fun classify(exceptions: List<ExceptionData>): Double {
        val gradleExceptionMessages = exceptions.filter { it.exceptionClassName == "org.gradle.api.GradleException" }.map { it.message.trim().substringBefore("\n") }
        val knownCompilationRelatedExceptionClassNames = listOf("java.lang.ClassFormatError", "java.lang.ClassNotFoundException", "sbt.compiler.CompileFailed", "org.codehaus.groovy.control.MultipleCompilationErrorsException", "java.lang.LinkageError", "org.gradle.api.internal.tasks.compile.CompilationFailedException")

        return when {
            gradleExceptionMessages.any { it.startsWith("There were failing tests") } -> 1.0
            gradleExceptionMessages.any { it.startsWith("Condition failed with Exception") } -> 1.0
            gradleExceptionMessages.any { it.contains("Checkstyle rule violation") } -> 1.0
            exceptions.any {
                val messageSummary = it.message.trim().toLowerCase().substringBefore("\n")
                messageSummary.contains("compil") && (messageSummary.contains("error") || messageSummary.contains("fail"))
            } -> 0.97
            exceptions.any { knownCompilationRelatedExceptionClassNames.contains(it.exceptionClassName) } -> 0.95
            gradleExceptionMessages.any { it.contains("fail") && it.contains("test") } -> 0.95
            exceptions.any { it.failedTaskGoalName != null && it.failedTaskGoalName.contains("codenarc") } && gradleExceptionMessages.any { it.contains("violation") } -> 0.95
            exceptions.any { it.failedTaskTypeOrMojoClassName != null && it.failedTaskTypeOrMojoClassName.contains("Test") && it.message.contains("fail") } -> 0.95
            else -> 0.05
        }
    }
}

object FailureClassifier {

    // TODO(ew): May need a threshold input and probability score output
    fun classifyFailure(exceptions: List<ExceptionData>): FailureClassification {
        val threshold = 0.9

        // TODO: run all classifiers for each input and choose the highest likelihood
        return when {
            exceptions.any { it.exceptionClassName == "org.gradle.api.BuildCancelledException" } -> FailureClassification.UNCLASSIFIED
            VerificationFailureClassifier.classify(exceptions) >= threshold -> FailureClassification.VERIFICATION
            InfrastructureFailureClassifier.classify(exceptions) >= threshold -> FailureClassification.BUILD_ENVIRONMENT
            indicatesBuildConfigurationFailure(exceptions) -> FailureClassification.BUILD_CONFIGURATION
            else -> FailureClassification.UNCLASSIFIED
        }
    }

    private fun indicatesBuildConfigurationFailure(exceptions: List<ExceptionData>): Boolean {
        return when {
            exceptions.all { it.failedTaskGoalName == null } -> true
            exceptions.any { (it.exceptionClassName.startsWith("org.gradle") && it.exceptionClassName != "org.gradle.api.GradleException" && it.exceptionClassName != "org.gradle.process.internal.ExecException") } -> true
            else -> false
        }
    }
}
