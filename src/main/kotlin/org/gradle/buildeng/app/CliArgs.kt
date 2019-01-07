package org.gradle.buildeng.app

import com.beust.jcommander.Parameter
import com.beust.jcommander.converters.IntegerConverter
import com.beust.jcommander.converters.PathConverter
import java.nio.file.Path

class CliArgs {
    @Parameter(names = ["--server", "-s"], description = "URL of Gradle Enterprise server", required = true)
    var server: String? = null

    @Parameter(names = ["--username", "-u"], description = "Export API user")
    var username: String? = null

    @Parameter(names = ["--password", "-p"], description = "Export API password")
    var password: String? = null

    @Parameter(names = ["--days", "-d"], description = "Number of days of data to export", converter = IntegerConverter::class)
    var days = 30

    @Parameter(names = ["--file", "-f"], description = "Output file", converter = PathConverter::class, required = true)
    var outputFile: Path? = null

    @Parameter(names = ["--help", "-h"], description = "Prints usage information", help = true)
    var help: Boolean = false
}
