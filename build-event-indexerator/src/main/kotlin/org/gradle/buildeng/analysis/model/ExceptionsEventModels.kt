package org.gradle.buildeng.analysis.model

data class ExceptionData(
        val exceptionId: String,
        val className: String,
        val message: String,
        val stackTrace: StackTrace,
        val causes: List<String>,
        val metadata: String?,
        val classLevelAnnotations: List<String>
)

data class StackTrace(
        val stackTraceId: String,
        val stackFrames: List<StackFrame>
)

data class StackFrame(
        val stackFrameId: String,
        val declaringClass: String?,
        val methodName: String?,
        val fileName: String?,
        val lineNumber: Int?,
        val fileRef: String?
)
