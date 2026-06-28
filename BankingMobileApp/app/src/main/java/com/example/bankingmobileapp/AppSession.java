package com.example.bankingmobileapp;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSession {
    private static final String PREFS = "banking_demo_session";
    private static final String USER_ID = "user_id";
    private static final String USER_EMAIL = "user_email";
    private static final String ACCOUNT_ID = "account_id";
    private static final String ACCOUNT_NUMBER = "account_number";
    private static final String ACCOUNT_BALANCE = "account_balance";

    private AppSession() {
    }

    public static void saveUserId(Context context, String userId) {
        putOrRemove(context, USER_ID, userId);
    }

    public static String getUserId(Context context) {
        return getString(context, USER_ID);
    }

    public static void saveUserEmail(Context context, String email) {
        putOrRemove(context, USER_EMAIL, email);
    }

    public static String getUserEmail(Context context) {
        return getString(context, USER_EMAIL);
    }

    public static boolean hasUser(Context context) {
        return !getUserId(context).isEmpty();
    }

    public static void saveAccountNumber(Context context, String accountNumber) {
        String normalized = normalize(accountNumber);
        String current = getAccountNumber(context);
        SharedPreferences.Editor editor = prefs(context).edit();
        if (normalized.isEmpty()) {
            editor.remove(ACCOUNT_NUMBER).remove(ACCOUNT_ID).remove(ACCOUNT_BALANCE).apply();
            return;
        }
        if (!normalized.equals(current)) {
            editor.remove(ACCOUNT_ID).remove(ACCOUNT_BALANCE);
        }
        editor.putString(ACCOUNT_NUMBER, normalized).apply();
    }

    public static String getAccountNumber(Context context) {
        return getString(context, ACCOUNT_NUMBER);
    }

    public static void saveAccountId(Context context, String accountId) {
        putOrRemove(context, ACCOUNT_ID, accountId);
    }

    public static String getAccountId(Context context) {
        return getString(context, ACCOUNT_ID);
    }

    public static void saveAccountBalance(Context context, String balance) {
        putOrRemove(context, ACCOUNT_BALANCE, balance);
    }

    public static String getAccountBalance(Context context) {
        return getString(context, ACCOUNT_BALANCE);
    }

    public static boolean hasAccount(Context context) {
        return !getAccountNumber(context).isEmpty();
    }

    public static void clearAccount(Context context) {
        prefs(context).edit()
                .remove(ACCOUNT_ID)
                .remove(ACCOUNT_NUMBER)
                .remove(ACCOUNT_BALANCE)
                .apply();
    }

    public static void clearSession(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String getString(Context context, String key) {
        String value = prefs(context).getString(key, "");
        return value == null ? "" : value.trim();
    }

    private static void putOrRemove(Context context, String key, String value) {
        String normalized = normalize(value);
        SharedPreferences.Editor editor = prefs(context).edit();
        if (normalized.isEmpty()) {
            editor.remove(key);
        } else {
            editor.putString(key, normalized);
        }
        editor.apply();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
