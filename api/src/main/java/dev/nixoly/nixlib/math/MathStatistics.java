package dev.nixoly.nixlib.math;

import java.util.Arrays;

public final class MathStatistics {

    private MathStatistics() {
    }

    public static double average(double[] values, int length) {
        if (length <= 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            sum += values[i];
        }
        return sum / length;
    }

    public static double populationStdev(double[] values, int length, double mean) {
        if (length <= 0) {
            return 0.0;
        }
        double sumSquared = 0.0;
        for (int i = 0; i < length; i++) {
            double delta = values[i] - mean;
            sumSquared += delta * delta;
        }
        return Math.sqrt(sumSquared / length);
    }

    public static double median(double[] values, int length) {
        if (length <= 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        return medianOfSortedRange(sorted, 0, length);
    }

    public static double medianAbsoluteDeviation(double[] values, int length) {
        if (length <= 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        double median = medianOfSortedRange(sorted, 0, length);
        double[] deviations = new double[length];
        for (int i = 0; i < length; i++) {
            deviations[i] = Math.abs(sorted[i] - median);
        }
        Arrays.sort(deviations);
        return medianOfSortedRange(deviations, 0, length);
    }

    public static double tightestWindowMedian(double[] values, int length, int windowSize) {
        if (length <= 0 || windowSize <= 0) {
            return 0.0;
        }
        int effective = Math.min(windowSize, length);
        double[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        int bestStart = findTightestWindowStart(sorted, length, effective);
        return medianOfSortedRange(sorted, bestStart, bestStart + effective);
    }

    public static double tightestWindowMad(double[] values, int length, int windowSize) {
        if (length <= 0 || windowSize <= 0) {
            return 0.0;
        }
        int effective = Math.min(windowSize, length);
        double[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        int bestStart = findTightestWindowStart(sorted, length, effective);
        double median = medianOfSortedRange(sorted, bestStart, bestStart + effective);
        double[] deviations = new double[effective];
        for (int i = 0; i < effective; i++) {
            deviations[i] = Math.abs(sorted[bestStart + i] - median);
        }
        Arrays.sort(deviations);
        return medianOfSortedRange(deviations, 0, effective);
    }

    public static int outlierCount(double[] values, int length, double scale) {
        if (length < 2) {
            return 0;
        }
        double[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        int half = length / 2;
        double q1 = medianOfSortedRange(sorted, 0, half);
        double q3 = medianOfSortedRange(sorted, half, length);
        double iqr = Math.abs(q1 - q3);
        double lowBound = q1 - scale * iqr;
        double highBound = q3 + scale * iqr;
        int count = 0;
        for (int i = 0; i < length; i++) {
            double v = sorted[i];
            if (v < lowBound || v > highBound) {
                count++;
            }
        }
        return count;
    }

    private static int findTightestWindowStart(double[] sorted, int length, int windowSize) {
        int bestStart = 0;
        double bestRange = Double.POSITIVE_INFINITY;
        int lastStart = length - windowSize;
        for (int start = 0; start <= lastStart; start++) {
            double range = sorted[start + windowSize - 1] - sorted[start];
            if (range < bestRange) {
                bestRange = range;
                bestStart = start;
            }
        }
        return bestStart;
    }

    private static double medianOfSortedRange(double[] sorted, int from, int toExclusive) {
        int length = toExclusive - from;
        if (length <= 0) {
            return 0.0;
        }
        if ((length & 1) == 0) {
            return (sorted[from + length / 2] + sorted[from + length / 2 - 1]) * 0.5;
        }
        return sorted[from + length / 2];
    }
}
