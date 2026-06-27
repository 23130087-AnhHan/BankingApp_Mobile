package com.example.bankingmobileapp;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSession {
    private static final String PREFS = "banking_demo_session";
    private static final String USER_ID = "user_id";
    private static final String ACCOUNT_NUMBER = "account_number";

    private AppSession() {
    }

    public static void saveUserId(Context context, String userId) {
        prefs(context).edit().putString(USER_ID, userId).apply();
    }

    public static String getUserId(Context context) {
        return prefs(context).getString(USER_ID, "1");
    }

    public static void saveAccountNumber(Context context, String accountNumber) {
        prefs(context).edit().putString(ACCOUNT_NUMBER, accountNumber).apply();
    }

    public static String getAccountNumber(Context context) {
        return prefs(context).getString(ACCOUNT_NUMBER, "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
