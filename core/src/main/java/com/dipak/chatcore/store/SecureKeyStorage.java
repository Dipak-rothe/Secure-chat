package com.dipak.chatcore.store;

/**
 * Platform-specific secure storage interface.
 * Desktop: encrypts to a file in the user's app-data folder.
 * Android: uses EncryptedSharedPreferences / Android Keystore.
 *
 * Implementations MUST be thread-safe.
 * Callers should NEVER log values returned from these methods.
 */
public interface SecureKeyStorage {

    /**
     * Store a value under the given key.
     * @param key   storage identifier (e.g. "access_token")
     * @param value value to persist securely
     */
    void put(String key, String value);

    /**
     * Retrieve a stored value.
     * @return the value, or null if not found
     */
    String get(String key);

    /**
     * Remove a stored value.
     */
    void remove(String key);

    /**
     * Remove all stored values (e.g. on logout).
     */
    void clear();
}
