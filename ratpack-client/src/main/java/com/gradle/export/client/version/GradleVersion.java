package com.gradle.export.client.version;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.regex.Pattern;

/**
 * Specifically represents a version of the Gradle build tool.
 */
public final class GradleVersion implements Comparable<GradleVersion> {

    private static final Pattern PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?(?:-\\w+)?(?:-\\w+)?(?:\\+\\w+)?");

    private final VersionNumber number;

    public GradleVersion(String versionString) {
        this.number = VersionNumber.parse(versionString);
    }

    public GradleVersion validate() {
        if (!isValid()) {
            throw new IllegalStateException("Gradle version " + asString() + " is not a valid Gradle version.");
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

    public GradleVersion majorAndMinor() {
        return parse(number.majorAndMinor().asString());
    }

    public boolean isValid() {
        return !number.isUnknown() && PATTERN.matcher(asString()).matches();
    }

    public boolean isFinal() {
        return number.isFinal();
    }

    public boolean isReleaseCandidate() {
        return number.isReleaseCandidate();
    }

    // Intentionally doesn't consider qualifier
    public boolean isAtLeast(GradleVersion version) {
        return number.isAtLeast(version.number);
    }

    // Intentionally doesn't consider qualifier
    public boolean isAtMost(GradleVersion version) {
        return number.isAtMost(version.number);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(GradleVersion o) {
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

        GradleVersion that = (GradleVersion) o;
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
        return "GradleVersion{" + number + '}';
    }

    public static GradleVersion parse(String version) {
        return new GradleVersion(version);
    }

}
