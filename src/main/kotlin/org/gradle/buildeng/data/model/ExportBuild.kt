package org.gradle.buildeng.data.model

import java.time.Instant

data class ExportBuild(val agent: String, val startDateTime: Instant, val durationMillis: Int, val id: String, val isCI: Boolean)
