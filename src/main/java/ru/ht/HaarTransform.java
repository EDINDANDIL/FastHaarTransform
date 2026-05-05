package ru.ht;

import ru.ht.util.TransformOperation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class HaarTransform {

    private static final double SQRT_2 = Math.sqrt(2.0);

    public static void fhtAllLevels(double[] signal) {fht(signal, Integer.MAX_VALUE);}

    public static void fht(double[] signal) {fht(signal, 1);}

    public static void fht(double[] signal, int levels) {
        validatePowerOfTwoLength(signal.length);

        if (levels < 0) {throw new IllegalArgumentException("Количество уровней не может быть отрицательным");}

        double[] temp = new double[signal.length];
        int length = signal.length;
        int level = 0;

        while (length > 1 && level < levels) {
            fhtLevel(signal, temp, length);
            length /= 2;
            level++;
        }
    }

    private static void transformLevel(double[] data, double[] temp, int length, TransformOperation op) {
        int half = length / 2;
        for (int i = 0; i < half; i++) {
            op.apply(data[2 * i], data[2 * i + 1], temp, i, half);
        }
        System.arraycopy(temp, 0, data, 0, length);
    }

    private static void fhtLevel(double[] signal, double[] temp, int length) {
        transformLevel(signal, temp, length, (a, b, t, i, half) -> {
            t[i] = (a + b) / SQRT_2;
            t[half + i] = (a - b) / SQRT_2;
        });
    }

    private static void inverseFhtLevel(double[] coeffs, double[] temp, int length) {
        int half = length / 2;
        for (int i = 0; i < half; i++) {
            double sum = coeffs[i];
            double diff = coeffs[half + i];
            temp[2 * i] = (sum + diff) / SQRT_2;
            temp[2 * i + 1] = (sum - diff) / SQRT_2;
        }
        System.arraycopy(temp, 0, coeffs, 0, length);
    }

    private static void validatePowerOfTwoLength(int n) {
        if (n == 0 || (n & (n - 1)) != 0) throw new IllegalArgumentException("Длина массива должна быть степенью двойки");
    }

    public static void inverseFht(double[] coeffs) {
        inverseFht(coeffs, Integer.MAX_VALUE);
    }

    public static void inverseFht(double[] coeffs, int levels) {
        validatePowerOfTwoLength(coeffs.length);

        if (levels < 0) {
            throw new IllegalArgumentException("Количество уровней не может быть отрицательным");
        }

        int actualLevels = 0;
        int length = coeffs.length;
        while (length > 1 && actualLevels < levels) {
            length /= 2;
            actualLevels++;
        }

        if (actualLevels == 0) {
            return;
        }

        double[] temp = new double[coeffs.length];
        length = coeffs.length >> (actualLevels - 1);

        while (length <= coeffs.length) {
            inverseFhtLevel(coeffs, temp, length);
            length *= 2;
        }
    }

    // тут тоже для all
    public static void naiveHt(double[] signal) {
        int n = signal.length;

        validatePowerOfTwoLength(n);
        double[][] h = buildHaarMatrix(n);
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;

            for (int j = 0; j < n; j++) {
                sum += h[i][j] * signal[j];
            }
            result[i] = sum;
        }
        System.arraycopy(result, 0, signal, 0, n);
    }

    private static double[][] buildHaarMatrix(int n) {

        validatePowerOfTwoLength(n);
        double[][] h = new double[n][n];
        for (int col = 0; col < n; col++) {
            double[] basis = new double[n];
            basis[col] = 1.0;
            fhtAllLevels(basis);
            for (int row = 0; row < n; row++) {
                h[row][col] = basis[row];
            }
        }
        return h;
    }

    public static void fht2d(double[][] matrix, int levels) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        if (levels < 0) {
            throw new IllegalArgumentException("Количество уровней не может быть отрицательным");
        }

        int currentRows = rows;
        int currentCols = cols;

        for (int level = 0; level < levels && currentRows > 1 && currentCols > 1; level++) {
            transformRows(matrix, currentRows, currentCols);
            transformColumns(matrix, currentRows, currentCols);

            currentRows /= 2;
            currentCols /= 2;
        }
    }

    public static void fht2d(double[][] matrix) {
        fht2d(matrix, Integer.MAX_VALUE);
    }

    static void inverseFht2d(double[][] matrix) {
        inverseFht2d(matrix, Integer.MAX_VALUE);
    }

    static void inverseFht2d(double[][] matrix, int levels) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        if (levels < 0) {
            throw new IllegalArgumentException("Количество уровней не может быть отрицательным");
        }

        int actualLevels = actualLevelCount(rows, cols, levels);

        for (int level = actualLevels - 1; level >= 0; level--) {
            int blockRows = rows >> level;
            int blockCols = cols >> level;

            inverseColumns(matrix, blockRows, blockCols);
            inverseRows(matrix, blockRows, blockCols);
        }
    }

    private static int actualLevelCount(int rows, int cols, int levels) {
        int actualLevels = 0;

        while (actualLevels < levels && rows > 1 && cols > 1) {
            rows /= 2;
            cols /= 2;
            actualLevels++;
        }
        return actualLevels;
    }

    private static void transformRows(double[][] matrix, int rows, int cols) {
        for (int y = 0; y < rows; y++) {
            double[] row = new double[cols];
            System.arraycopy(matrix[y], 0, row, 0, cols);
            fht(row);
            System.arraycopy(row, 0, matrix[y], 0, cols);
        }
    }

    private static void transformColumns(double[][] matrix, int rows, int cols) {
        for (int x = 0; x < cols; x++) {
            double[] column = new double[rows];
            for (int y = 0; y < rows; y++) {column[y] = matrix[y][x];}
            fht(column);
            for (int y = 0; y < rows; y++) {matrix[y][x] = column[y];}
        }
    }

    private static void inverseRows(double[][] matrix, int rows, int cols) {
        for (int y = 0; y < rows; y++) {
            double[] row = new double[cols];
            System.arraycopy(matrix[y], 0, row, 0, cols);
            inverseFht(row, 1);
            System.arraycopy(row, 0, matrix[y], 0, cols);
        }
    }

    private static void inverseColumns(double[][] matrix, int rows, int cols) {
        for (int x = 0; x < cols; x++) {
            double[] column = new double[rows];
            for (int y = 0; y < rows; y++) {column[y] = matrix[y][x];}
            inverseFht(column, 1);
            for (int y = 0; y < rows; y++) {matrix[y][x] = column[y];}
        }
    }

    public static double[][] imageResourceToGrayMatrix(String resourceName) throws IOException {
        try (InputStream input = HaarTransform.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            BufferedImage image = ImageIO.read(input);

            int width = image.getWidth();
            int height = image.getHeight();

            double[][] matrix = new double[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);

                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;

                    double gray = 0.299 * r + 0.587 * g + 0.114 * b;

                    matrix[y][x] = gray;
                }
            }
            return matrix;
        }
    }

    static BufferedImage grayMatrixToImage(double[][] matrix) {
        int height = matrix.length;
        int width = matrix[0].length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = clampToByte(matrix[y][x]);
                int rgb = (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    static BufferedImage haarMatrixToImage(double[][] matrix, int levels) {
        int height = matrix.length;
        int width = matrix[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int blockHeight = height >> levels;
        int blockWidth = width >> levels;
        for (int by = 0; by < (1 << levels); by++) {
            for (int bx = 0; bx < (1 << levels); bx++) {
                int startY = by * blockHeight;
                int startX = bx * blockWidth;
                double maxAbs = 0.0;
                for (int y = startY; y < startY + blockHeight; y++) {
                    for (int x = startX; x < startX + blockWidth; x++) {
                        maxAbs = Math.max(maxAbs, Math.abs(matrix[y][x]));
                    }
                }
                if (maxAbs == 0.0) maxAbs = 1.0;
                for (int y = startY; y < startY + blockHeight; y++) {
                    for (int x = startX; x < startX + blockWidth; x++) {
                        double normalized = Math.abs(matrix[y][x]) / maxAbs;
                        int gray = clampToByte(normalized * 255.0);
                        int rgb = (gray << 16) | (gray << 8) | gray;
                        image.setRGB(x, y, rgb);
                    }
                }
            }
        }
        return image;
    }

    private static int clampToByte(double value) {
        int rounded = (int) Math.round(value);
        if (rounded < 0) {
            return 0;
        }
        return Math.min(rounded, 255);
    }

    public static void main(String[] args) throws IOException {

        // Пример с изображением
        double[][] matrix = imageResourceToGrayMatrix("/velikodushnyy_pyos.png");
        int levels = 2;
        fht2d(matrix, levels);
        BufferedImage coefficients = haarMatrixToImage(matrix, levels);
        ImageIO.write(coefficients, "png", new File("src/main/resources/haar_coefficients.png"));
        inverseFht2d(matrix, levels);
        BufferedImage result = grayMatrixToImage(matrix);
        ImageIO.write(result, "png", new File("src/main/resources/restored.png"));
    }
}
