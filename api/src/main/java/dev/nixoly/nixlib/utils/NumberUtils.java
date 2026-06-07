package dev.nixoly.nixlib.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public final class NumberUtils {

    private static final DecimalFormatSymbols US_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);
    private static final DecimalFormat COMPACT_2DP = new DecimalFormat("#,##0.##", US_SYMBOLS);

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private NumberUtils() {}

    public static Optional<Integer> tryInt(String s) {
        if (s == null) return Optional.empty();
        try { return Optional.of(Integer.parseInt(s.trim())); }
        catch (NumberFormatException e) { return Optional.empty(); }
    }

    public static Optional<Long> tryLong(String s) {
        if (s == null) return Optional.empty();
        try { return Optional.of(Long.parseLong(s.trim())); }
        catch (NumberFormatException e) { return Optional.empty(); }
    }

    public static Optional<Double> tryDouble(String s) {
        if (s == null) return Optional.empty();
        try { return Optional.of(Double.parseDouble(s.trim())); }
        catch (NumberFormatException e) { return Optional.empty(); }
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static String formatWithCommas(long value) {
        return COMPACT_2DP.format(value);
    }

    public static String formatCompact(long value) {
        if (value < 1000) return Long.toString(value);
        int idx = 0;
        double v = value;
        while (v >= 1000 && idx < SUFFIXES.length - 1) {
            v /= 1000d;
            idx++;
        }
        return COMPACT_2DP.format(v) + SUFFIXES[idx];
    }

    public static String formatPercent(double ratio) {
        return COMPACT_2DP.format(ratio * 100d) + "%";
    }
}
