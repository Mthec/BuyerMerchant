package com.wurmonline.server.questions;

import java.math.BigDecimal;
import java.math.RoundingMode;

class WeightString {
    private static final BigDecimal mod = new BigDecimal(1000);

    static String toString(int weight) {
        BigDecimal bd = new BigDecimal(weight).setScale(3, RoundingMode.UNNECESSARY);
        bd = bd.divide(mod, RoundingMode.UNNECESSARY);
        return bd.stripTrailingZeros().toPlainString();
    }

    static int toInt(String str) {
        return new BigDecimal(str).multiply(mod).intValue();
    }
}
