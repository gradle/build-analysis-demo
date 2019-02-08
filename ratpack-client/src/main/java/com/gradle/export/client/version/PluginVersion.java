package com.gradle.export.client.version;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.regex.Pattern;

/**
 * Specifically represents a version of the build scan plugin.
 */
public final class PluginVersion implements Comparable<PluginVersion> {

    private static final Pattern PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?(?:-[\\w-]+)?");

    private final VersionNumber number;

    public PluginVersion(String versionString) {
        this.number = VersionNumber.parse(versionString);
    }

    public PluginVersion validate() {
        if (!isValid()) {
            throw new IllegalStateException("Plugin version " + asString() + " is not a valid plugin version.");
        } else {
            return this;
        }
    }

    public int getMajor() {
        return number.getMajor();
    }

    public int getMinor() {
        return number.getMinor();
    }

    public int getPatch() {
        return number.getPatch();
    }

    public PluginVersion normalize() {
        return parse(number.toBaseVersion().asString());
    }

    public PluginVersion majorAndMinor() {
        return parse(number.majorAndMinor().asString());
    }

    public boolean isValid() {
        return !number.isUnknown() && PATTERN.matcher(asString()).matches();
    }

    public boolean isReleaseCandidate() {
        return number.isReleaseCandidate();
    }

    public boolean isFinal() {
        return number.isFinal();
    }

    // Intentionally doesn't consider qualifier
    public boolean isAtLeast(PluginVersion version) {
        return number.isAtLeast(version.number);
    }

    // Intentionally doesn't consider qualifier
    public boolean isAtMost(PluginVersion version) {
        return number.isAtMost(version.number);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(PluginVersion o) {
        return number.compareTo(o.number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginVersion that = (PluginVersion) o;
        return number.equals(that.number);
    }

    @Override
    public int hashCode() {
        return number.hashCode();
    }

    @JsonValue
    public String asString() {
        return number.asString();
    }

    @Override
    public String toString() {
        return "PluginVersion{" + number + '}';
    }

    public static PluginVersion parse(String version) {
        return new PluginVersion(version);
    }

}
