package com.dipak.desktop.store;

import com.dipak.chatcore.store.SecureKeyStorage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

/**
 * Desktop implementation of SecureKeyStorage.
 * Stores tokens encrypted with AES-256-GCM in the user's AppData directory.
 * Plaintext tokens are NEVER written to disk.
 */
public class FileSecureKeyStorage implements SecureKeyStorage {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final Path storageFile;
    private final SecretKey encryptionKey;
    private final Properties properties = new Properties();

    public FileSecureKeyStorage() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.trim().isEmpty()) {
            appData = System.getProperty("user.home") + File.separator + ".securechat";
        } else {
            appData = appData + File.separator + "SecureChat";
        }
        File dir = new File(appData);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.storageFile = Paths.get(appData, "keys.dat");
        this.encryptionKey = deriveHostKey();
        load();
    }

    // Constructor for testing with custom path
    public FileSecureKeyStorage(Path customFile) {
        this.storageFile = customFile;
        this.encryptionKey = deriveHostKey();
        load();
    }

    @Override
    public synchronized void put(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
        save();
    }

    @Override
    public synchronized String get(String key) {
        return properties.getProperty(key);
    }

    @Override
    public synchronized void remove(String key) {
        properties.remove(key);
        save();
    }

    @Override
    public synchronized void clear() {
        properties.clear();
        save();
    }

    private SecretKey deriveHostKey() {
        try {
            String seed = System.getProperty("user.name") + ":" + System.getProperty("user.home");
            byte[] salt = "SecureChatDesktopHostSaltV1".getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(seed.toCharArray(), salt, 65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive host encryption key", e);
        }
    }

    private void load() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try {
            byte[] allBytes = Files.readAllBytes(storageFile);
            if (allBytes.length <= GCM_IV_LENGTH) {
                return;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(allBytes, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherBytes = new byte[allBytes.length - GCM_IV_LENGTH];
            System.arraycopy(allBytes, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            try (ByteArrayInputStream in = new ByteArrayInputStream(plainBytes)) {
                properties.load(in);
            }
        } catch (Exception e) {
            // If decryption fails, start fresh
            properties.clear();
        }
    }

    private void save() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            properties.store(out, null);
            byte[] plainBytes = out.toByteArray();

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            byte[] combined = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, GCM_IV_LENGTH, cipherBytes.length);

            Files.write(storageFile, combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist secure storage", e);
        }
    }
}
