package ru.ht.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import static ru.ht.HaarTransform.fhtAllLevels;
import static ru.ht.HaarTransform.inverseFht;

public class SignalStructureExperiment {

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 650;
    private static final int LEFT = 90;
    private static final int RIGHT = 90;
    private static final int TOP = 70;
    private static final int BOTTOM = 90;

    private static final int SIGNAL_LENGTH = 2048;
    private static final int APPROXIMATION_COEFFICIENTS = 64;

    public static void main(String[] args) throws IOException {
        double[] signal = generateSignal();

        double[] coefficients = signal.clone();
        fhtAllLevels(coefficients);

        double[] approximation = coefficients.clone();
        keepFirstCoefficients(approximation);
        inverseFht(approximation);

        double[] restored = coefficients.clone();
        inverseFht(restored);

        File resources = new File("src/main/resources");
        if (!resources.exists() && !resources.mkdirs()) {
            throw new IOException("Cannot create resources directory: " + resources.getAbsolutePath());
        }

        drawSignalChart(
                signal,
                "Исходный сигнал",
                new File(resources, "signal_original.png")
        );

        drawSignalChart(
                approximation,
                "Аппроксимация Хаара по первым " + APPROXIMATION_COEFFICIENTS + " коэффициентам",
                new File(resources, "signal_haar_approximation.png")
        );

        drawSignalChart(
                restored,
                "Полное восстановление после обратного преобразования Хаара",
                new File(resources, "signal_restored.png")
        );
    }

    private static double[] generateSignal() {
        double[] signal = new double[SIGNAL_LENGTH];
        Random random = new Random(42);

        double[] levels = {
                112.0, 112.0, 124.0, 124.0,
                138.0, 138.0, 124.0, 112.0,
                112.0, 152.0, 152.0, 138.0,
                124.0, 124.0, 112.0, 112.0,
                138.0, 138.0, 152.0, 152.0,
                124.0, 112.0, 112.0, 124.0,
                138.0, 152.0, 138.0, 124.0,
                112.0, 112.0, 124.0, 138.0
        };
        int segmentLength = SIGNAL_LENGTH / levels.length;

        for (int segment = 0; segment < levels.length; segment++) {
            int from = segment * segmentLength;
            int to = segment == levels.length - 1 ? SIGNAL_LENGTH : from + segmentLength;

            for (int i = from; i < to; i++) {
                double value = levels[segment]
                        + slowOscillation(i)
                        + random.nextGaussian() * 3.5;

                if (random.nextDouble() < 0.01) {
                    double spike = 12.0 + random.nextDouble() * 18.0;
                    value += random.nextBoolean() ? spike : -spike;
                }

                signal[i] = value;
            }
        }
        return signal;
    }

    private static double slowOscillation(int index) {return 4.0 * Math.sin(2.0 * Math.PI * index / 512.0);}

    private static void keepFirstCoefficients(double[] coefficients) {
        if (SignalStructureExperiment.APPROXIMATION_COEFFICIENTS < 1 || SignalStructureExperiment.APPROXIMATION_COEFFICIENTS > coefficients.length) {
            throw new IllegalArgumentException("Количество коэффициентов должно быть от 1 до длины сигнала");
        }
        for (int i = SignalStructureExperiment.APPROXIMATION_COEFFICIENTS; i < coefficients.length; i++) {
            coefficients[i] = 0.0;
        }
    }

    private static void drawSignalChart(
            double[] signal,
            String title,
            File output
    ) throws IOException {
        BufferedImage image = createCanvas();
        Graphics2D g = image.createGraphics();
        prepareGraphics(g);

        double signalMin = min(signal) - 8.0;
        double signalMax = max(signal) + 8.0;

        drawTitle(g, title);
        drawAxes(g);
        drawYTicks(g, signalMin, signalMax);
        drawXTicks(g, signal.length);

        drawSeries(g, signal, signalMin, signalMax, new Color(33, 102, 172));
        drawLegend(g);

        g.dispose();
        ImageIO.write(image, "png", output);
    }

    private static void drawSeries(
            Graphics2D g,
            double[] values,
            double min,
            double max,
            Color color
    ) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1.6f));

        for (int i = 0; i < values.length - 1; i++) {
            int x1 = xForIndex(i, values.length);
            int y1 = yForValue(values[i], min, max);
            int x2 = xForIndex(i + 1, values.length);
            int y2 = yForValue(values[i + 1], min, max);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static BufferedImage createCanvas() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.dispose();
        return image;
    }

    private static void prepareGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Times New Roman", Font.PLAIN, 20));
    }

    private static void drawTitle(Graphics2D g, String title) {
        drawCentered(g, title, WIDTH / 2, 35, new Font("Times New Roman", Font.BOLD, 28));
    }

    private static void drawAxes(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(LEFT, HEIGHT - BOTTOM, WIDTH - RIGHT, HEIGHT - BOTTOM);
        g.drawLine(LEFT, TOP, LEFT, HEIGHT - BOTTOM);

        drawCentered(g, "Номер отсчета", (LEFT + WIDTH - RIGHT) / 2, HEIGHT - 25, new Font("Times New Roman", Font.PLAIN, 22));

        Font old = g.getFont();
        g.setFont(new Font("Times New Roman", Font.PLAIN, 22));
        g.rotate(-Math.PI / 2);
        g.drawString("Значение сигнала", -HEIGHT / 2 - 45, 30);
        g.rotate(Math.PI / 2);
        g.setFont(old);
    }

    private static void drawYTicks(Graphics2D g, double min, double max) {
        g.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(1f));
        Font font = new Font("Times New Roman", Font.PLAIN, 18);

        for (int i = 0; i <= 5; i++) {
            double value = min + (max - min) * i / 5.0;
            int y = yForValue(value, min, max);
            g.drawLine(LEFT - 5, y, LEFT, y);
            drawRightAligned(g, formatNumber(value), LEFT - 10, y + 6, font);
            g.setColor(new Color(230, 230, 230));
            g.drawLine(LEFT + 1, y, WIDTH - RIGHT, y);
            g.setColor(Color.DARK_GRAY);
        }
    }

    private static void drawXTicks(Graphics2D g, int n) {
        g.setColor(Color.DARK_GRAY);
        Font font = new Font("Times New Roman", Font.PLAIN, 18);
        int step = n > 1024 ? 512 : Math.max(1, n / 8);
        for (int i = 0; i < n; i += step) {
            int x = xForIndex(i, n);
            g.drawLine(x, HEIGHT - BOTTOM, x, HEIGHT - BOTTOM + 5);
            drawCentered(g, Integer.toString(i), x, HEIGHT - BOTTOM + 28, font);
        }
        if ((n - 1) % step != 0) {
            int x = xForIndex(n - 1, n);
            g.drawLine(x, HEIGHT - BOTTOM, x, HEIGHT - BOTTOM + 5);
            drawCentered(g, Integer.toString(n - 1), x, HEIGHT - BOTTOM + 28, font);
        }
    }

    private static void drawLegend(Graphics2D g) {
        int x = WIDTH - RIGHT - 160;
        int y = TOP + 20;
        drawLegendLine(g, x, y, new Color(33, 102, 172), "Сигнал");
    }

    private static void drawLegendLine(Graphics2D g, int x, int y, Color color, String text) {
        g.setColor(color);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(x, y - 5, x + 28, y - 5);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        g.drawString(text, x + 38, y);
    }

    private static int xForIndex(int index, int n) {
        if (n == 1) {
            return LEFT;
        }
        double ratio = index / (double) (n - 1);
        return LEFT + (int) Math.round(ratio * (WIDTH - LEFT - RIGHT));
    }

    private static int yForValue(double value, double min, double max) {
        double ratio = (value - min) / (max - min);
        return HEIGHT - BOTTOM - (int) Math.round(ratio * (HEIGHT - TOP - BOTTOM));
    }

    private static double min(double[] values) {
        double result = Double.POSITIVE_INFINITY;
        for (double value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static double max(double[] values) {
        double result = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static void drawCentered(Graphics2D g, String text, int x, int y, Font font) {
        Font old = g.getFont();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x - metrics.stringWidth(text) / 2, y);
        g.setFont(old);
    }

    private static void drawRightAligned(Graphics2D g, String text, int x, int y, Font font) {
        Font old = g.getFont();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x - metrics.stringWidth(text), y);
        g.setFont(old);
    }

}
