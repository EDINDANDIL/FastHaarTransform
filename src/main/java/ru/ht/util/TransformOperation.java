package ru.ht.util;

@FunctionalInterface
public interface TransformOperation {
    void apply(double a, double b, double[] temp, int i, int half);
}