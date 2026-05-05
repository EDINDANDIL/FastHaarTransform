package ru.ht;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static ru.ht.HaarTransform.*;

public class HaarTransformTest {

    private static final double EPS = 1e-9;

    static Stream<double[]> signals() {
        return Stream.of(
                new double[]{1, 2},
                new double[]{1, 2, 4, 4},
                new double[]{4, 6, 10, 12},
                new double[]{5, 5, 5, 5, 20, 20, 20, 20},
                new double[]{1, -1, 2, -2, 3, -3, 4, -4},
                new double[]{0, 0, 0, 0, 0, 0, 0, 0}
        );
    }

    @Test
    void fhtShouldTransformOneLevelOnly() {
        double[] signal = new double[]{4, 6, 10, 12};

        fht(signal);

        assertArrayEquals(
                new double[]{
                        10 / Math.sqrt(2.0),
                        22 / Math.sqrt(2.0),
                        -2 / Math.sqrt(2.0),
                        -2 / Math.sqrt(2.0)
                },
                signal,
                EPS
        );
    }

    @Test
    void fhtWithLevelsShouldTransformRequestedNumberOfLevels() {
        double[] signal = new double[]{4, 6, 10, 12};

        fht(signal, 2);

        assertArrayEquals(
                new double[]{
                        16,
                        -6,
                        -2 / Math.sqrt(2.0),
                        -2 / Math.sqrt(2.0)
                },
                signal,
                EPS
        );
    }

    @Test
    void inverseFhtWithLevelsShouldRestoreOriginalSignal() {
        double[] signal = new double[]{4, 6, 10, 12, 20, 22, 30, 32};
        double[] transformed = signal.clone();

        fht(transformed, 2);
        inverseFht(transformed, 2);

        assertArrayEquals(
                signal,
                transformed,
                EPS
        );
    }

    @Test
    void inverseFht2dWithLevelsShouldRestoreOriginalMatrix() {
        double[][] matrix = new double[][]{
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 15, 16}
        };
        double[][] transformed = copy(matrix);

        fht2d(transformed, 2);
        inverseFht2d(transformed, 2);

        assertMatrixEquals(matrix, transformed);
    }

    private static double[][] copy(double[][] source) {
        double[][] copy = new double[source.length][source[0].length];

        for (int y = 0; y < source.length; y++) {
            System.arraycopy(source[y], 0, copy[y], 0, source[y].length);
        }

        return copy;
    }

    private static void assertMatrixEquals(double[][] expected, double[][] actual) {
        for (int y = 0; y < expected.length; y++) {
            assertArrayEquals(
                    expected[y],
                    actual[y],
                    EPS
            );
        }
    }

    @ParameterizedTest
    @MethodSource("signals")
    void fhtAllLevelsShouldEqualNaiveHt(double[] signal) {
        double[] signal1 = signal.clone();
        fhtAllLevels(signal1);
        double[] signal2 = signal.clone();
        naiveHt(signal2);

        assertArrayEquals(
                signal1,
                signal2,
                EPS
        );
    }

    @ParameterizedTest
    @MethodSource("signals")
    void inverseFhtAllLevelsShouldRestoreOriginalSignal(double[] signal) {
        double[] transformed = signal.clone();

        fhtAllLevels(transformed);
        inverseFht(transformed);

        assertArrayEquals(
                signal,
                transformed,
                EPS
        );
    }

    @ParameterizedTest
    @MethodSource("signals")
    void naiveHtAndFhtAllLevelsShouldRestoreSameSignal(double[] signal) {
        double[] signal1 = signal.clone();
        double[] signal2 = signal.clone();

        naiveHt(signal1);
        fhtAllLevels(signal2);

        inverseFht(signal1);
        inverseFht(signal2);

        assertArrayEquals(
                signal1,
                signal2,
                EPS
        );

        assertArrayEquals(
                signal,
                signal1,
                EPS
        );
    }
}
