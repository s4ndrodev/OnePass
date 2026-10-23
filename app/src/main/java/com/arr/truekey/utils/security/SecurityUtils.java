package com.arr.truekey.utils.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class SecurityUtils {

    private static final String PREFS_FILE = "secure_db_prefs";
    private static final String KEY_PASSPHRASE = "db_passphrase";

    public static char[] getOrCreatePassphrase(Context context) {
        try {
            MasterKey masterKey =
                    new MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

            SharedPreferences prefs =
                    EncryptedSharedPreferences.create(
                            context,
                            PREFS_FILE,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

            String stored = prefs.getString(KEY_PASSPHRASE, null);
            if (stored == null) {
                stored = generateRandomPassphrase();
                prefs.edit().putString(KEY_PASSPHRASE, stored).apply();
            }
            return stored.toCharArray();

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("No se pudo obtener la passphrase segura", e);
        }
    }

    private static String generateRandomPassphrase() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
