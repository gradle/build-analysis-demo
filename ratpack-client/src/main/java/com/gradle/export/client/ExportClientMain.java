package com.gradle.export.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.gradle.export.client.util.StandaloneRatpackHarness;

import java.io.PrintWriter;
import java.io.Writer;

public class ExportClientMain {

    public static void main(String[] args) throws Exception {
        ExportClientSpec spec = ExportClientSpec.from(parseArgs(args));
        ExportClient client = new ExportClient(spec);

        try (Writer writer = new PrintWriter(spec.outputFile.toFile())) {
            StandaloneRatpackHarness.execute(
                client.export(new BuildCacheMetricsReduction(writer, spec.obfuscate))
            );
        }
    }

    private static CliArgs parseArgs(String... args) {
        CliArgs cliArgs = new CliArgs();
        JCommander jCommander = new JCommander(cliArgs);
        try {
            jCommander.parse(args);

            if (cliArgs.help) {
                jCommander.usage();
                System.exit(0);
            }
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        return cliArgs;
    }
}
