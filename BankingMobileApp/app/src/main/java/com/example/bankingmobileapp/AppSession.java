package com.example.bankingmobileapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bankingmobileapp.model.AccountResponse;

public final class AppSession {
    private static final String PREFS = "banking_demo_session";
    private static final String IS_LOGGED_IN = "is_logged_in";
    private static final String USER_ID = "user_id";
    private static final String USER_EMAIL = "user_email";
    private static final String REMEMBERED_USER_ID = "remembered_user_id";
    private static final String REMEMBERED_DISPLAY_NAME = "remembered_display_name";
    private static final String ACCOUNT_ID = "account_id";
    private static final String ACCOUNT_NUMBER = "account_number";
    private static final String ACCOUNT_BALANCE = "account_balance";
    private static final String AUTH_TOKEN = "auth_token";

    private AppSession() {
    }

    public static void saveLoginState(Context context, boolean isLoggedIn) {
        prefs(context).edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply();
    }

    public static boolean isLoggedIn(Context context) {
        return prefs(context).getBoolean(IS_LOGGED_IN, false);
    }

    public static void saveUserId(Context context, String userId) {
        prefs(context).edit().putString(USER_ID, userId).apply();
    }

    public static String getUserId(Context context) {
        return prefs(context).getString(USER_ID, "");
    }

    public static void saveUserEmail(Context context, String email) {
        prefs(context).edit().putString(USER_EMAIL, email).apply();
    }

    public static String getUserEmail(Context context) {
        return prefs(context).getString(USER_EMAIL, "");
    }

    public static void saveRememberedUser(Context context, String userId, String displayName) {
        prefs(context).edit()
                .putString(REMEMBERED_USER_ID, userId == null ? "" : userId)
                .putString(REMEMBERED_DISPLAY_NAME, displayName == null ? "" : displayName)
                .apply();
    }

    public static String getRememberedUserId(Context context) {
        String rememberedUserId = prefs(context).getString(REMEMBERED_USER_ID, "");
        return rememberedUserId == null || rememberedUserId.isEmpty() ? getUserId(context) : rememberedUserId;
    }

    public static String getRememberedDisplayName(Context context) {
        String displayName = prefs(context).getString(REMEMBERED_DISPLAY_NAME, "");
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        String userId = getRememberedUserId(context);
        return userId.isEmpty() ? "" : "User ID " + userId;
    }

    public static boolean hasRememberedUser(Context context) {
        return !getRememberedUserId(context).isEmpty();
    }

    public static void clearRememberedUser(Context context) {
        prefs(context).edit()
                .remove(REMEMBERED_USER_ID)
                .remove(REMEMBERED_DISPLAY_NAME)
                .apply();
    }

    public static void saveAccountId(Context context, String accountId) {
        prefs(context).edit().putString(ACCOUNT_ID, accountId).apply();
    }

    public static String getAccountId(Context context) {
        return prefs(context).getString(ACCOUNT_ID, "");
    }

    public static void saveAccountNumber(Context context, String accountNumber) {
        prefs(context).edit().putString(ACCOUNT_NUMBER, accountNumber).apply();
    }

    public static String getAccountNumber(Context context) {
        return prefs(context).getString(ACCOUNT_NUMBER, "");
    }

    public static void saveAccountBalance(Context context, String balance) {
        prefs(context).edit().putString(ACCOUNT_BALANCE, balance).apply();
    }

    public static String getAccountBalance(Context context) {
        return prefs(context).getString(ACCOUNT_BALANCE, "");
    }

    public static void saveAuthToken(Context context, String token) {
        prefs(context).edit().putString(AUTH_TOKEN, token).apply();
    }

    public static String getAuthToken(Context context) {
        return prefs(context).getString(AUTH_TOKEN, "");
    }

    public static void clearAuthToken(Context context) {
        prefs(context).edit().remove(AUTH_TOKEN).apply();
    }

    public static void saveAccount(Context context, AccountResponse account) {
        if (account == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs(context).edit();
        if (account.userId != null) {
            editor.putString(USER_ID, String.valueOf(account.userId));
        }
        if (account.accountId != null) {
            editor.putString(ACCOUNT_ID, String.valueOf(account.accountId));
        }
        editor.putString(ACCOUNT_NUMBER, account.accountNumber == null ? "" : account.accountNumber);
        editor.putString(ACCOUNT_BALANCE, account.availableBalance == null ? "" : account.availableBalance.toPlainString());
        editor.apply();
    }

    public static boolean hasAccount(Context context) {
        return !getAccountNumber(context).isEmpty();
    }

    public static void clearLoginState(Context context) {
        prefs(context).edit()
                .putBoolean(IS_LOGGED_IN, false)
                .remove(AUTH_TOKEN)
                .apply();
    }

    public static void clearSession(Context context) {
        clearAll(context);
    }

    public static void clearAll(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
