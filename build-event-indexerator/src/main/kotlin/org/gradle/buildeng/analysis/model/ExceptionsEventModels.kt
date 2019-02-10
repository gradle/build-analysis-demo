package org.gradle.buildeng.analysis.model

data class ExceptionData(
        val exceptionId: String,
        val className: String,
        val message: String,
        val stackTraceId: String?,
        val causes: List<String>,
        val metadata: String?,
        val classLevelAnnotations: List<String>
)
