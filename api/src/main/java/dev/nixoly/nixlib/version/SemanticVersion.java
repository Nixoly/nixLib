package dev.nixoly.nixlib.version;

import java.util.Objects;

public final class SemanticVersion implements Comparable<SemanticVersion> {

    public static final SemanticVersion ZERO = new SemanticVersion(0, 0, 0);

    private final int major;
    private final int minor;
    private final int patch;

    private SemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SemanticVersion of(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("semver components must be non-negative");
        }
        return new SemanticVersion(major, minor, patch);
    }

    public static SemanticVersion parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("version string is null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("version string is empty");
        }
        if (trimmed.charAt(0) == 'v' || trimmed.charAt(0) == 'V') {
            trimmed = trimmed.substring(1);
        }
        int dash = trimmed.indexOf('-');
        int plus = trimmed.indexOf('+');
        int cut = -1;
        if (dash >= 0) cut = dash;
        if (plus >= 0 && (cut < 0 || plus < cut)) cut = plus;
        if (cut >= 0) trimmed = trimmed.substring(0, cut);

        String[] parts = trimmed.split("\\.");
        int major = digits(parts, 0);
        int minor = digits(parts, 1);
        int patch = digits(parts, 2);
        return new SemanticVersion(major, minor, patch);
    }

    public static SemanticVersion parseOr(String value, SemanticVersion fallback) {
        try {
            return parse(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int digits(String[] parts, int idx) {
        if (idx >= parts.length) return 0;
        String segment = parts[idx];
        StringBuilder buf = new StringBuilder(segment.length());
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c < '0' || c > '9') break;
            buf.append(c);
        }
        if (buf.length() == 0) return 0;
        try {
            return Integer.parseInt(buf.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int major() { return major; }

    public int minor() { return minor; }

    public int patch() { return patch; }

    public boolean isOlderThan(SemanticVersion other) {
        return compareTo(other) < 0;
    }

    public boolean isNewerThan(SemanticVersion other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(SemanticVersion o) {
        int cmp = Integer.compare(major, o.major);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(minor, o.minor);
        if (cmp != 0) return cmp;
        return Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticVersion that)) return false;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
