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
        return medianDeviationOfSortedRange(sorted, 0, length);
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
        return medianDeviationOfSortedRange(sorted, bestStart, bestStart + effective);
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

    // In a sorted range the deviations from the median shrink towards the median from the left and grow
    // away from it on the right, so they come out in order by walking outwards from the median with two
    // cursors. That gives their median without a second array or a second sort.
    private static double medianDeviationOfSortedRange(double[] sorted, int from, int toExclusive) {
        int length = toExclusive - from;
        double median = medianOfSortedRange(sorted, from, toExclusive);
        int right = from;
        while (right < toExclusive && sorted[right] < median) {
            right++;
        }
        int left = right - 1;
        int target = length / 2;
        double previous = 0.0;
        double current = 0.0;
        for (int taken = 0; taken <= target; taken++) {
            previous = current;
            if (left < from || (right < toExclusive && sorted[right] - median <= median - sorted[left])) {
                current = sorted[right++] - median;
            } else {
                current = median - sorted[left--];
            }
        }
        return (length & 1) == 0 ? (previous + current) * 0.5 : current;
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
