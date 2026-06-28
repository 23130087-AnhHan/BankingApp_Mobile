package com.example.bankingmobileapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.AuthResponse;

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
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_EXPIRES_AT = "token_expires_at";

    private AppSession() {
    }

    public static void saveLoginState(Context context, boolean isLoggedIn) {
        prefs(context).edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply();
    }

    public static boolean isLoggedIn(Context context) {
        return prefs(context).getBoolean(IS_LOGGED_IN, false);
    }

    public static void saveUserId(Context context, String userId) {
        putOrRemove(context, USER_ID, userId);
    }

    public static String getUserId(Context context) {
        return getString(context, USER_ID);
    }

    public static boolean hasUser(Context context) {
        return !getUserId(context).isEmpty();
    }

    public static void saveUserEmail(Context context, String email) {
        putOrRemove(context, USER_EMAIL, email);
    }

    public static String getUserEmail(Context context) {
        return getString(context, USER_EMAIL);
    }

    public static void saveRememberedUser(Context context, String userId, String displayName) {
        String normalizedUserId = normalize(userId);
        String normalizedDisplayName = normalize(displayName);
        prefs(context).edit()
                .putString(REMEMBERED_USER_ID, normalizedUserId)
                .putString(REMEMBERED_DISPLAY_NAME, normalizedDisplayName)
                .apply();
    }

    public static String getRememberedUserId(Context context) {
        String rememberedUserId = getString(context, REMEMBERED_USER_ID);
        return rememberedUserId.isEmpty() ? getUserId(context) : rememberedUserId;
    }

    public static String getRememberedDisplayName(Context context) {
        String displayName = getString(context, REMEMBERED_DISPLAY_NAME);
        if (!displayName.isEmpty()) {
            return displayName;
        }
        String email = getUserEmail(context);
        if (!email.isEmpty()) {
            return email;
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

    public static void saveAuthToken(Context context, String token) {
        SecureTokenStore.put(prefs(context), AUTH_TOKEN, token);
    }

    public static String getAuthToken(Context context) {
        return SecureTokenStore.get(prefs(context), AUTH_TOKEN);
    }

    public static void clearAuthToken(Context context) {
        prefs(context).edit().remove(AUTH_TOKEN).remove(REFRESH_TOKEN).remove(TOKEN_EXPIRES_AT).apply();
    }

    public static String getRefreshToken(Context context) {
        return SecureTokenStore.get(prefs(context), REFRESH_TOKEN);
    }

    public static void saveAuth(Context context, AuthResponse auth) {
        if (auth == null || auth.accessToken == null || auth.accessToken.trim().isEmpty()) return;
        saveAuthToken(context, auth.accessToken);
        SecureTokenStore.put(prefs(context), REFRESH_TOKEN, auth.refreshToken);
        long expiresIn = auth.expiresIn == null ? 0L : auth.expiresIn;
        prefs(context).edit().putLong(TOKEN_EXPIRES_AT,
                System.currentTimeMillis() + Math.max(0L, expiresIn - 30L) * 1000L).apply();
        if (auth.userId != null) saveUserId(context, String.valueOf(auth.userId));
        saveUserEmail(context, auth.email);
        saveRememberedUser(context, auth.userId == null ? "" : String.valueOf(auth.userId), auth.displayName);
    }

    public static boolean hasValidSession(Context context) {
        return isLoggedIn(context) && !getAuthToken(context).isEmpty() && !getRefreshToken(context).isEmpty();
    }

    public static void saveAccount(Context context, AccountResponse account) {
        if (account == null) {
            return;
        }
        if (account.userId != null) {
            saveUserId(context, String.valueOf(account.userId));
        }
        if (account.accountId != null) {
            saveAccountId(context, String.valueOf(account.accountId));
        }
        saveAccountNumber(context, account.accountNumber);
        if (account.availableBalance != null) {
            saveAccountBalance(context, account.availableBalance.toPlainString());
        }
    }

    public static void clearLoginState(Context context) {
        prefs(context).edit()
                .putBoolean(IS_LOGGED_IN, false)
                .remove(AUTH_TOKEN)
                .remove(REFRESH_TOKEN)
                .remove(TOKEN_EXPIRES_AT)
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
