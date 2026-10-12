package com.dipak.securechat.store;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.dipak.chatcore.store.SecureKeyStorage;

/**
 * Android implementation of SecureKeyStorage using EncryptedSharedPreferences
 * backed by the hardware Android Keystore.
 */
public class SharedPreferencesKeyStorage implements SecureKeyStorage {

    private final SharedPreferences prefs;

    public SharedPreferencesKeyStorage(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            this.prefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_chat_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise EncryptedSharedPreferences", e);
        }
    }

    @Override
    public synchronized void put(String key, String value) {
        if (value == null) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putString(key, value).apply();
        }
    }

    @Override
    public synchronized String get(String key) {
        return prefs.getString(key, null);
    }

    @Override
    public synchronized void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    @Override
    public synchronized void clear() {
        prefs.edit().clear().apply();
    }
}
