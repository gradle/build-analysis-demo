package com.gradle.export.client;

import java.net.URI;
import java.nio.file.Path;

class ExportClientSpec {

    final URI serverUri;
    final String username;
    final String password;
    final int days;
    final Path outputFile;
    final boolean obfuscate;
    final int workers;

    private ExportClientSpec(URI serverUri, String username, String password, int days, Path outputFile, boolean obfuscate, int workers) {
        this.serverUri = serverUri;
        this.username = username;
        this.password = password;
        this.days = days;
        this.outputFile = outputFile;
        this.obfuscate = obfuscate;
        this.workers = workers;
    }

    static ExportClientSpec from(CliArgs cliArgs) {
        return new ExportClientSpec(cliArgs.server, cliArgs.username, cliArgs.password, cliArgs.days, cliArgs.outputFile, cliArgs.obfuscate, cliArgs.workers);
    }

}
