package com.urbano.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    private static final String KES_SYMBOL = "KES ";
    private static final Locale KENYA_LOCALE = new Locale("en", "KE");
    private static final int SCALE = 2;

    public static BigDecimal toBigDecimal(double amount) {
        return BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal toBigDecimal(String amount) {
        return new BigDecimal(amount).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static String formatKes(BigDecimal amount) {
        if (amount == null) return KES_SYMBOL + "0.00";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(KENYA_LOCALE);
        return formatter.format(amount);
    }

    public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.subtract(b).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }
}