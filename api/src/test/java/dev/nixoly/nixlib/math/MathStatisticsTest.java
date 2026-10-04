package dev.nixoly.nixlib.math;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MathStatisticsTest {

    private static final double EPS = 1e-9;

    @Test
    void averageReturnsMean() {
        double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
        assertThat(MathStatistics.average(values, values.length)).isCloseTo(3.0, within(EPS));
    }

    @Test
    void averageHandlesZeroLength() {
        assertThat(MathStatistics.average(new double[0], 0)).isCloseTo(0.0, within(EPS));
        assertThat(MathStatistics.average(new double[]{1.0, 2.0}, 0)).isCloseTo(0.0, within(EPS));
    }

    @Test
    void averageHonoursPartialLength() {
        double[] values = {1.0, 1.0, 1.0, 100.0};
        assertThat(MathStatistics.average(values, 3)).isCloseTo(1.0, within(EPS));
    }

    @Test
    void populationStdevMatchesKnownValue() {
        double[] values = {2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0};
        double mean = MathStatistics.average(values, values.length);
        assertThat(MathStatistics.populationStdev(values, values.length, mean)).isCloseTo(2.0, within(EPS));
    }

    @Test
    void populationStdevZeroForConstantSeries() {
        double[] values = {7.0, 7.0, 7.0, 7.0};
        assertThat(MathStatistics.populationStdev(values, values.length, 7.0)).isCloseTo(0.0, within(EPS));
    }

    @Test
    void medianOddLength() {
        double[] values = {5.0, 1.0, 9.0, 3.0, 7.0};
        assertThat(MathStatistics.median(values, values.length)).isCloseTo(5.0, within(EPS));
    }

    @Test
    void medianEvenLengthIsAverageOfMiddlePair() {
        double[] values = {4.0, 1.0, 3.0, 2.0};
        assertThat(MathStatistics.median(values, values.length)).isCloseTo(2.5, within(EPS));
    }

    @Test
    void medianAbsoluteDeviationOnSymmetricSet() {
        double[] values = {1.0, 1.0, 2.0, 2.0, 4.0, 6.0, 9.0};
        assertThat(MathStatistics.medianAbsoluteDeviation(values, values.length)).isCloseTo(1.0, within(EPS));
    }

    @Test
    void tightestWindowMedianPicksDenseCluster() {
        double[] values = {199.0, 201.0, 200.0, 198.0, 200.0, 700.0, 1500.0, 850.0};
        assertThat(MathStatistics.tightestWindowMedian(values, values.length, 5))
                .isCloseTo(200.0, within(EPS));
    }

    @Test
    void tightestWindowMadDropsOutliers() {
        double[] values = {199.0, 201.0, 200.0, 198.0, 200.0, 700.0, 1500.0, 850.0};
        assertThat(MathStatistics.tightestWindowMad(values, values.length, 5))
                .as("expected tight cluster MAD to drop the high outliers")
                .isLessThanOrEqualTo(2.0);
    }

    @Test
    void tightestWindowFallsBackWhenWindowExceedsLength() {
        double[] values = {10.0, 12.0, 14.0};
        assertThat(MathStatistics.tightestWindowMedian(values, values.length, 10))
                .isCloseTo(12.0, within(EPS));
    }

    @Test
    void outlierCountFlagsExtremeValues() {
        double[] values = {10.0, 10.0, 10.0, 11.0, 11.0, 12.0, 12.0, 1000.0};
        assertThat(MathStatistics.outlierCount(values, values.length, 1.5))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void outlierCountReturnsZeroForTinyInputs() {
        assertThat(MathStatistics.outlierCount(new double[]{5.0}, 1, 3.0)).isZero();
        assertThat(MathStatistics.outlierCount(new double[0], 0, 3.0)).isZero();
    }

    private static org.assertj.core.data.Offset<Double> within(double v) {
        return org.assertj.core.data.Offset.offset(v);
    }

    @Test
    void madMatchesSortingTheDeviations() {
        Random random = new Random(7);
        for (int length = 1; length <= 40; length++) {
            double[] values = new double[length];
            for (int i = 0; i < length; i++) {
                values[i] = random.nextInt(9) == 0 ? values[Math.max(0, i - 1)] : random.nextGaussian() * 10;
            }
            assertThat(MathStatistics.medianAbsoluteDeviation(values, length))
                    .as("length %d", length)
                    .isCloseTo(sortedDeviationMedian(values, 0, length), within(EPS));
        }
    }

    @Test
    void windowMadMatchesSortingTheDeviations() {
        double[] values = {9.0, 1.0, 1.2, 0.9, 1.1, 30.0, 1.05, -4.0};
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        // The tightest 5-wide window of the sorted values is 0.9 .. 1.2.
        assertThat(MathStatistics.tightestWindowMad(values, values.length, 5))
                .isCloseTo(sortedDeviationMedian(sorted, 1, 6), within(EPS));
    }

    private static double sortedDeviationMedian(double[] values, int from, int to) {
        double[] range = Arrays.copyOfRange(values, from, to);
        Arrays.sort(range);
        double median = medianOf(range);
        double[] deviations = new double[range.length];
        for (int i = 0; i < range.length; i++) {
            deviations[i] = Math.abs(range[i] - median);
        }
        Arrays.sort(deviations);
        return medianOf(deviations);
    }

    private static double medianOf(double[] sorted) {
        int n = sorted.length;
        return (n & 1) == 0 ? (sorted[n / 2] + sorted[n / 2 - 1]) * 0.5 : sorted[n / 2];
    }
}
