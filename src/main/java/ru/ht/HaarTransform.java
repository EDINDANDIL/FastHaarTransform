package ru.ht;

import ru.ht.util.TransformOperation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Реализация преобразования Хаара для одномерных сигналов и двумерных данных.
 *
 * <p>Все основные методы изменяют переданный массив или матрицу на месте. Это
 * значит, что результат записывается в тот же объект, который был передан в
 * метод, без создания нового массива результата для вызывающего кода.</p>
 *
 * <p>Одномерные методы работают с массивами, длина которых является степенью
 * двойки. Двумерные методы применяются к матрицам, у которых количество строк и
 * столбцов также являются степенями двойки.</p>
 */
public class HaarTransform {

    private static final double SQRT_2 = Math.sqrt(2.0);

    /**
     * Выполняет полное быстрое преобразование Хаара для всех возможных уровней.
     *
     * <p>После выполнения в начале массива находится самый грубый коэффициент
     * аппроксимации, а дальше расположены коэффициенты детализации разных
     * уровней. Исходные значения сигнала перезаписываются коэффициентами.</p>
     *
     * @param signal исходный сигнал; длина массива должна быть степенью двойки
     * @throws IllegalArgumentException если длина массива не является степенью двойки
     */
    public static void fhtAllLevels(double[] signal) {fht(signal, Integer.MAX_VALUE);}

    /**
     * Выполняет один уровень быстрого преобразования Хаара.
     *
     * <p>Метод обрабатывает соседние пары значений: для каждой пары вычисляются
     * коэффициент аппроксимации и коэффициент детализации. Аппроксимации
     * записываются в первую половину массива, детали — во вторую.</p>
     *
     * @param signal исходный сигнал; длина массива должна быть степенью двойки
     * @throws IllegalArgumentException если длина массива не является степенью двойки
     */
    public static void fht(double[] signal) {fht(signal, 1);}

    /**
     * Выполняет заданное число уровней быстрого преобразования Хаара.
     *
     * <p>На каждом следующем уровне преобразуется только первая половина текущей
     * области, то есть ранее полученные коэффициенты аппроксимации. Если
     * {@code levels} больше максимально возможного числа уровней, выполняется
     * полное разложение.</p>
     *
     * @param signal исходный сигнал; результат записывается в этот же массив
     * @param levels количество уровней преобразования
     * @throws IllegalArgumentException если длина массива не является степенью двойки
     * @throws IllegalArgumentException если {@code levels} отрицательное
     */
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

    private static void fhtLevel(double[] signal, double[] temp, int length) {
        transformLevel(signal, temp, length, (a, b, t, i, half) -> {
            t[i] = (a + b) / SQRT_2;
            t[half + i] = (a - b) / SQRT_2;
        });
    }

    private static void transformLevel(double[] data, double[] temp, int length, TransformOperation op){

        int half = length / 2;
        for (int i = 0; i < half; i++) {
            op.apply(data[2 * i], data[2 * i + 1], temp, i, half);
        }
        System.arraycopy(temp, 0, data, 0, length);
    }

    private static void validatePowerOfTwoLength(int n) {
        if (n == 0 || (n & (n - 1)) != 0) throw new IllegalArgumentException("Длина массива должна быть степенью двойки");
    }

    /**
     * Выполняет полное обратное быстрое преобразование Хаара.
     *
     * <p>Метод восстанавливает исходный сигнал из коэффициентов, полученных
     * методом {@link #fhtAllLevels(double[])}. Коэффициенты перезаписываются
     * восстановленными значениями сигнала.</p>
     *
     * @param coeffs коэффициенты преобразования Хаара
     * @throws IllegalArgumentException если длина массива не является степенью двойки
     */
    public static void inverseFht(double[] coeffs) {
        inverseFht(coeffs, Integer.MAX_VALUE);
    }

    /**
     * Выполняет обратное преобразование Хаара для заданного числа уровней.
     *
     * <p>Количество уровней должно соответствовать тому, сколько уровней было
     * использовано при прямом преобразовании. Например, если сигнал был
     * преобразован методом {@code fht(signal, 3)}, то для восстановления нужно
     * вызвать {@code inverseFht(signal, 3)}.</p>
     *
     * @param coeffs коэффициенты преобразования; результат записывается в этот же массив
     * @param levels количество уровней обратного преобразования
     * @throws IllegalArgumentException если длина массива не является степенью двойки
     * @throws IllegalArgumentException если {@code levels} отрицательное
     */
    public static void inverseFht(double[] coeffs, int levels) {
        validatePowerOfTwoLength(coeffs.length);

        if (levels < 0) {throw new IllegalArgumentException("Количество уровней не может быть отрицательным");}

        int actualLevels = 0;
        int length = coeffs.length;

        while (length > 1 && actualLevels < levels) {
            length /= 2;
            actualLevels++;
        }

        if (actualLevels == 0) return;

        double[] temp = new double[coeffs.length];
        length = coeffs.length >> (actualLevels - 1);

        while (length <= coeffs.length) {
            inverseFhtLevel(coeffs, temp, length);
            length *= 2;
        }
    }

    private static void inverseFhtLevel(double[] coeffs, double[] temp, int length) {
        inverseTransformLevel(coeffs, temp, length, (sum, diff, t, i, half) -> {
            t[2 * i] = (sum + diff) / SQRT_2;
            t[2 * i + 1] = (sum - diff) / SQRT_2;
        });
    }

    private static void inverseTransformLevel(double[] data, double[] temp, int length, TransformOperation op) {
        int half = length / 2;
        for (int i = 0; i < half; i++) {
            op.apply(data[i], data[half + i], temp, i, half);
        }
        System.arraycopy(temp, 0, data, 0, length);
    }

    /**
     * Выполняет преобразование Хаара прямым матричным способом.
     *
     * <p>Метод строит матрицу Хаара и умножает ее на исходный сигнал. Он нужен
     * главным образом для сравнения с быстрым преобразованием в эксперименте,
     * потому что имеет более высокую вычислительную сложность.</p>
     *
     * @param signal исходный сигнал; результат записывается в этот же массив
     * @throws IllegalArgumentException если длина массива не является степенью двойки
     */
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

    /**
     * Выполняет двумерное быстрое преобразование Хаара для заданного числа уровней.
     *
     * <p>На каждом уровне преобразование сначала применяется к строкам текущего
     * блока, затем к столбцам. После уровня изображение или матрица разделяется
     * на область аппроксимации и области деталей. Следующий уровень применяется
     * только к области аппроксимации в левом верхнем углу.</p>
     *
     * @param matrix матрица значений; результат записывается в эту же матрицу
     * @param levels количество уровней двумерного преобразования
     * @throws IllegalArgumentException если число строк или столбцов не является степенью двойки
     * @throws IllegalArgumentException если {@code levels} отрицательное
     */
    public static void fht2d(double[][] matrix, int levels) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        validatePowerOfTwoLength(rows);
        validatePowerOfTwoLength(cols);
        if (levels < 0) {throw new IllegalArgumentException("Количество уровней не может быть отрицательным");}

        int currentRows = rows;
        int currentCols = cols;

        for (int level = 0; level < levels && currentRows > 1 && currentCols > 1; level++) {
            transformRows(matrix, currentRows, currentCols);
            transformColumns(matrix, currentRows, currentCols);
            currentRows /= 2;
            currentCols /= 2;
        }
    }

    /**
     * Выполняет полное двумерное быстрое преобразование Хаара.
     *
     * <p>Преобразование продолжается до тех пор, пока текущая область
     * аппроксимации может быть разделена пополам по строкам и столбцам.</p>
     *
     * @param matrix матрица значений; число строк и столбцов должно быть степенью двойки
     */
    public static void fht2d(double[][] matrix) {fht2d(matrix, Integer.MAX_VALUE);}

    /**
     * Выполняет полное обратное двумерное преобразование Хаара.
     *
     * @param matrix матрица коэффициентов; результат записывается в эту же матрицу
     */
    static void inverseFht2d(double[][] matrix) {inverseFht2d(matrix, Integer.MAX_VALUE);}

    /**
     * Выполняет обратное двумерное преобразование Хаара для заданного числа уровней.
     *
     * <p>Метод восстанавливает матрицу после вызова {@link #fht2d(double[][], int)}
     * с тем же количеством уровней.</p>
     *
     * @param matrix матрица коэффициентов
     * @param levels количество уровней обратного преобразования
     * @throws IllegalArgumentException если {@code levels} отрицательное
     */
    static void inverseFht2d(double[][] matrix, int levels) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        if (levels < 0) {throw new IllegalArgumentException("Количество уровней не может быть отрицательным");}

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

    /**
     * Загружает изображение из ресурсов проекта и переводит его в матрицу яркости.
     *
     * <p>Каждый пиксель преобразуется в одно число по стандартной формуле
     * яркости: красная, зеленая и синяя компоненты берутся с разными весами.
     * Полученная матрица затем может быть передана в {@link #fht2d(double[][], int)}.</p>
     *
     * @param resourceName путь к ресурсу, например {@code "/image.png"}
     * @return матрица яркости изображения
     * @throws IOException если ресурс не найден или изображение нельзя прочитать
     */
    public static double[][] imageResourceToGrayMatrix(String resourceName) throws IOException {
        try (InputStream input = HaarTransform.class.getResourceAsStream(resourceName)) {

            if (input == null) throw new IOException("Resource not found: " + resourceName);

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

    /**
     * Преобразует матрицу яркости обратно в черно-белое изображение.
     *
     * @param matrix матрица значений яркости
     * @return изображение, в котором каждое значение матрицы записано как серый пиксель
     */
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

    /**
     * Строит изображение коэффициентов двумерного преобразования Хаара.
     *
     * <p>Коэффициенты разных областей нормируются отдельно, чтобы на итоговом
     * изображении были видны как крупные, так и мелкие детали. Это изображение
     * предназначено для иллюстрации структуры коэффициентов, а не для обратного
     * восстановления.</p>
     *
     * @param matrix матрица коэффициентов Хаара
     * @param levels количество уровней, использованное при преобразовании
     * @return изображение для визуального анализа коэффициентов
     */
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
        if (rounded < 0) return 0;
        return Math.min(rounded, 255);
    }

    public static void main(String[] args) throws IOException {
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
