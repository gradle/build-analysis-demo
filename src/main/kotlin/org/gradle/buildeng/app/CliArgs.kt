package org.gradle.buildeng.app

import com.beust.jcommander.Parameter
import com.beust.jcommander.converters.IntegerConverter
import com.beust.jcommander.converters.PathConverter
import com.beust.jcommander.converters.URIConverter

import java.net.URI
import java.nio.file.Path

class CliArgs {
    @Parameter(names = ["--server", "-s"], description = "Url of Gradle Enterprise server", converter = URIConverter::class, required = true)
    var server: URI? = null

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
