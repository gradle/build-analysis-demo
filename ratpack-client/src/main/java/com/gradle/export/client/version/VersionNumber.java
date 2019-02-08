package com.gradle.export.client.version;

/**
 * Represents, parses, and compares version numbers. Supports a couple of different schemes: <ul> <li>MAJOR.MINOR.MICRO-QUALIFIER (the default).</li> <li>MAJOR.MINOR.MICRO.PATCH-QUALIFIER.</li> </ul>
 * <p>
 * <p>The {@link #parse} method handles missing parts and allows "." to be used instead of "-", and "_" to be used instead of "." for the patch number.
 * <p>
 * <p>This class considers missing parts to be 0, so that "1.0" == "1.0.0" == "1.0.0_0".</p>
 * <p>
 * <p>Note that this class considers "1.2.3-something" less than "1.2.3". Qualifiers are compared lexicographically ("1.2.3-alpha" < "1.2.3-beta") and case-insensitive ("1.2.3-alpha" <
 * "1.2.3.RELEASE").
 */
public final class VersionNumber implements Comparable<VersionNumber> {

    private static final DefaultScheme DEFAULT_SCHEME = new DefaultScheme(4);

    private final int major;
    private final int minor;
    private final int micro;
    private final int patch;
    private final String qualifier;
    private final String versionString;
    private final boolean unknown;

    private VersionNumber(int major, int minor, int micro, int patch, String qualifier, String versionString, boolean unknown) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.patch = patch;
        this.qualifier = qualifier;
        this.versionString = versionString;
        this.unknown = unknown;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public int getPatch() {
        return patch;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public boolean isReleaseCandidate() {
        return qualifier != null && qualifier.toUpperCase().startsWith("RC");
    }

    public boolean isFinal() {
        return !unknown && qualifier == null;
    }

    public VersionNumber toBaseVersion() {
        return unknown || qualifier == null ? this : DEFAULT_SCHEME.parseBaseVersion(versionString);
    }

    public VersionNumber majorAndMinor() {
        return unknown ? this : new VersionNumber(major, minor, 0, 0, null, String.format("%s.%s", major, minor), false);
    }

    public VersionNumber majorMinorAndMicro() {
        return unknown ? this : new VersionNumber(major, minor, micro, 0, null, String.format("%s.%s.%s", major, minor, micro), false);
    }

    // Intentionally doesn't consider qualifier
    public boolean isAtLeast(VersionNumber versionNumber) {
        if (unknown) {
            return false;
        }

        if (major > versionNumber.major) {
            return true;
        }
        if (major < versionNumber.major) {
            return false;
        }
        if (minor > versionNumber.minor) {
            return true;
        }
        if (minor < versionNumber.minor) {
            return false;
        }
        if (micro > versionNumber.micro) {
            return true;
        }
        if (micro < versionNumber.micro) {
            return false;
        }
        if (patch > versionNumber.patch) {
            return true;
        }
        if (patch < versionNumber.patch) {
            return false;
        }

        return true;
    }

    // Intentionally doesn't consider qualifier
    public boolean isAtMost(VersionNumber versionNumber) {
        if (unknown) {
            return false;
        }

        if (major < versionNumber.major) {
            return true;
        }
        if (major > versionNumber.major) {
            return false;
        }
        if (minor < versionNumber.minor) {
            return true;
        }
        if (minor > versionNumber.minor) {
            return false;
        }
        if (micro < versionNumber.micro) {
            return true;
        }
        if (micro > versionNumber.micro) {
            return false;
        }
        if (patch < versionNumber.patch) {
            return true;
        }
        if (patch > versionNumber.patch) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(VersionNumber other) {
        if (major != other.major) {
            return major - other.major;
        }
        if (minor != other.minor) {
            return minor - other.minor;
        }
        if (micro != other.micro) {
            return micro - other.micro;
        }
        if (patch != other.patch) {
            return patch - other.patch;
        }

        if (qualifier == null) {
            return other.qualifier == null ? 0 : 1;
        } else {
            return other.qualifier == null ? -1 : qualifier.toLowerCase().compareTo(other.qualifier.toLowerCase());
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VersionNumber && compareTo((VersionNumber) other) == 0;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + micro;
        result = 31 * result + patch;
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    public String asString() {
        return versionString;
    }

    @Override
    public String toString() {
        return asString();
    }

    public static VersionNumber parse(String versionString) {
        return DEFAULT_SCHEME.parse(versionString);
    }

    private static final class DefaultScheme {

        private final int depth;

        private DefaultScheme(int depth) {
            this.depth = depth;
        }

        private VersionNumber parse(String versionString) {
            return parse(versionString, false);
        }

        private VersionNumber parseBaseVersion(String versionString) {
            return parse(versionString, true);
        }

        private VersionNumber parse(String versionString, boolean baseVersionOnly) {
            if (versionString == null || versionString.length() == 0) {
                return unknownVersion(versionString);
            }

            Scanner scanner = new Scanner(versionString);

            int major;
            int minor = 0;
            int micro = 0;
            int patch = 0;

            if (!scanner.hasDigit()) {
                return unknownVersion(versionString);
            }

            major = scanner.scanDigit();
            if (scanner.isSeparatorAndDigit('.')) {
                scanner.skipSeparator();
                minor = scanner.scanDigit();
                if (scanner.isSeparatorAndDigit('.')) {
                    scanner.skipSeparator();
                    micro = scanner.scanDigit();
                    if (depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
                        scanner.skipSeparator();
                        patch = scanner.scanDigit();
                    }
                }
            }

            if (scanner.isEnd() || baseVersionOnly) {
                return new VersionNumber(major, minor, micro, patch, null, versionString.substring(0, scanner.pos), false);
            }

            if (scanner.isQualifier()) {
                scanner.skipSeparator();
                return new VersionNumber(major, minor, micro, patch, scanner.until('+'), versionString, false);
            }

            // The rest is metadata. Two versions that differ only in the build metadata, have the same precedence.
            // (see http://semver.org/#spec-item-10)
            return new VersionNumber(major, minor, micro, patch, null, versionString, false);
        }

        private static VersionNumber unknownVersion(String versionString) {
            return new VersionNumber(0, 0, 0, 0, null, versionString, true);
        }

        private static final class Scanner {

            private final String str;
            private int pos;

            private Scanner(String string) {
                this.str = string;
            }

            private boolean hasDigit() {
                return pos < str.length() && Character.isDigit(str.charAt(pos));
            }

            private boolean isSeparatorAndDigit(char... separators) {
                return pos < str.length() - 1 && oneOf(separators) && Character.isDigit(str.charAt(pos + 1));
            }

            private boolean oneOf(char... separators) {
                char current = str.charAt(pos);
                for (char separator : separators) {
                    if (current == separator) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isQualifier() {
                return pos < str.length() - 1 && oneOf('.', '-');
            }

            private int scanDigit() {
                int start = pos;
                while (hasDigit()) {
                    pos++;
                }
                return Integer.parseInt(str.substring(start, pos));
            }

            private boolean isEnd() {
                return pos == str.length();
            }

            private void skipSeparator() {
                pos++;
            }

            private String until(char ch) {
                if (pos == str.length()) {
                    return null;
                } else {
                    int until = pos;
                    while (until < str.length() && str.charAt(until) != ch) {
                        until++;
                    }
                    return str.substring(pos, until);
                }
            }

        }

    }

}

