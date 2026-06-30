package com.example.bankingmobileapp;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {
    public static String formatVnd(String amount) {
        if (amount == null || amount.isEmpty()) {
            return "0 VND";
        }
        try {
            BigDecimal value = new BigDecimal(amount);
            return formatVnd(value);
        } catch (Exception e) {
            return amount + " VND";
        }
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator(',');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(amount) + " VND";
    }
}
