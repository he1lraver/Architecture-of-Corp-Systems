package edu.masters.domain;

import java.math.BigDecimal;
import java.time.Year;

public final class Rules {
    private Rules() {}

    public static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new IllegalArgumentException(field + ": требуется от 1 до " + max + " символов");
        return value.trim();
    }

    public static int year(int value) {
        if (value < 1 || value > Year.now().getValue() + 1)
            throw new IllegalArgumentException("Недопустимый год");
        return value;
    }

    public static BigDecimal price(BigDecimal value) {
        if (value == null
                || value.signum() < 0
                || value.scale() > 2
                || value.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException(
                    "Цена: неотрицательное число, не более двух знаков после точки");
        return value;
    }

    public static int stock(int value) {
        if (value < 0) throw new IllegalArgumentException("Остаток не может быть отрицательным");
        return value;
    }
}
