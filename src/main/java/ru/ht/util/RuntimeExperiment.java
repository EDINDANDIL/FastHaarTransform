package ru.ht.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Random;

import static ru.ht.HaarTransform.*;

public class RuntimeExperiment {

    public static void main(String[] args) throws IOException {
        String fileName = "runtime.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("n,length,naive_ms,fht_ms");

            Random random = new Random(42);

            for (int n = 2; n <= 13; n++) {
                int length = 1 << n;

                double[] original = randomArray(length, random);

                double[] naive = original.clone();
                long naiveStart = System.nanoTime();
                naiveHt(naive);
                long naiveEnd = System.nanoTime();

                double[] fast = original.clone();
                long fastStart = System.nanoTime();
                fhtAllLevels(fast);
                long fastEnd = System.nanoTime();

                double naiveMs = (naiveEnd - naiveStart) / 1_000_000.0;
                double fastMs = (fastEnd - fastStart) / 1_000_000.0;

                writer.printf(Locale.US, "%d,%d,%.6f,%.6f%n", n, length, naiveMs, fastMs);

                System.out.printf(
                        "n=%d length=%d naive=%.6f ms fht=%.6f ms%n",
                        n, length, naiveMs, fastMs
                );
            }
        }
        System.out.println("Result saved to runtime.csv");
    }

    private static double[] randomArray(int length, Random random) {
        double[] array = new double[length];

        for (int i = 0; i < length; i++) {
            array[i] = random.nextDouble();
        }

        return array;
    }
}