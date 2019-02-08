package com.gradle.export.client.event;

import java.time.Instant;

@SuppressWarnings("WeakerAccess")
public class ExportBuild {

    public String buildId;
    public String pluginVersion;
    public String gradleVersion;
    public Instant timestamp;

}
