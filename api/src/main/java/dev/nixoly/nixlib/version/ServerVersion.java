package dev.nixoly.nixlib.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerVersion implements Comparable<ServerVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(?:^|\\D)(1)\\.(\\d{1,2})(?:\\.(\\d{1,2}))?");

    public static final ServerVersion V1_16_5  = of(1, 16, 5);
    public static final ServerVersion V1_17    = of(1, 17, 0);
    public static final ServerVersion V1_18    = of(1, 18, 0);
    public static final ServerVersion V1_19    = of(1, 19, 0);
    public static final ServerVersion V1_19_4  = of(1, 19, 4);
    public static final ServerVersion V1_20    = of(1, 20, 0);
    public static final ServerVersion V1_20_5  = of(1, 20, 5);
    public static final ServerVersion V1_20_6  = of(1, 20, 6);
    public static final ServerVersion V1_21    = of(1, 21, 0);
    public static final ServerVersion V1_21_4  = of(1, 21, 4);

    private final int major;
    private final int minor;
    private final int patch;

    private ServerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static ServerVersion of(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("version components must be non-negative");
        }
        return new ServerVersion(major, minor, patch);
    }

    public static ServerVersion parse(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("empty version string");
        }
        Matcher m = VERSION_PATTERN.matcher(input);
        if (!m.find()) {
            throw new IllegalArgumentException("unrecognised version: " + input);
        }
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        return of(major, minor, patch);
    }

    public int major() { return major; }
    public int minor() { return minor; }
    public int patch() { return patch; }

    public boolean isAtLeast(ServerVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean isOlderThan(ServerVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(ServerVersion o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) return c;
        c = Integer.compare(minor, o.minor);
        if (c != 0) return c;
        return Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerVersion v)) return false;
        return major == v.major && minor == v.minor && patch == v.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return patch == 0 ? major + "." + minor : major + "." + minor + "." + patch;
    }
}
