package org.gradle.buildeng.data.collection

import org.gradle.buildeng.app.CliArgs
import java.net.URI
import java.nio.file.Path

class ExportClientSpec constructor(val serverUri: URI, val username: String, val password: String, val days: Int, val outputFile: Path) {
    companion object {
        fun from(cliArgs: CliArgs): ExportClientSpec {
            return ExportClientSpec(cliArgs.server!!, cliArgs.username!!, cliArgs.password!!, cliArgs.days, cliArgs.outputFile!!)
        }
    }
}
