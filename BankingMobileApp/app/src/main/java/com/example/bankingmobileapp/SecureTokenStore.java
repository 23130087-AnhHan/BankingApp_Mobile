package com.example.bankingmobileapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureTokenStore {
    private static final String KEY_ALIAS = "banking_session_key";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private SecureTokenStore() {}

    static void put(SharedPreferences preferences, String key, String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                preferences.edit().remove(key).apply();
                return;
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
            String data = Base64.encodeToString(
                    cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            preferences.edit().putString(key, "v1:" + iv + ":" + data).apply();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot protect authentication token", exception);
        }
    }

    static String get(SharedPreferences preferences, String key) {
        String encoded = preferences.getString(key, "");
        if (encoded == null || encoded.isEmpty()) return "";
        try {
            String[] parts = encoded.split(":", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(128, Base64.decode(parts[1], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[2], Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            preferences.edit().remove(key).apply();
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
