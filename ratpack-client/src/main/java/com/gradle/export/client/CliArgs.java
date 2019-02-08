package com.gradle.export.client;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.IntegerConverter;
import com.beust.jcommander.converters.PathConverter;
import com.beust.jcommander.converters.URIConverter;

import java.net.URI;
import java.nio.file.Path;

final class CliArgs {

    @Parameter(names = {"--server", "-s"}, description = "Url of Gradle Enterprise server", converter = URIConverter.class, required = true)
    URI server;

    @Parameter(names = {"--username", "-u"}, description = "Export API user")
    String username;

    @Parameter(names = {"--password", "-p"}, description = "Export API password", password = true)
    String password;

    @Parameter(names = {"--days", "-d"}, description = "Number of days of data to export", converter = IntegerConverter.class)
    int days = 30;

    @Parameter(names = {"--file", "-f"}, description = "Output file", converter = PathConverter.class, required = true)
    Path outputFile;

    @Parameter(names = {"--obfuscate", "-o"}, description = "Obfuscate identifying information (root project name, task paths, etc) via hash")
    boolean obfuscate;

    @Parameter(names = {"--help", "-h"}, description = "Prints usage information", help = true)
    boolean help;

    @Parameter(names = {"--workers", "-w"}, description = "Maximum number of concurrent event processing requests", converter = IntegerConverter.class)
    int workers = 10;
}
