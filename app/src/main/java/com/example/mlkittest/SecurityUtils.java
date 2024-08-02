package com.example.mlkittest;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurityUtils {

    private static final String TAG = "SecurityUtils";
    private static final String KEY_ALIAS = "FaceDistancesKey";

    public void generateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        // Check if the key already exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return;
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        keyGenerator.generateKey();
    }

    public SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    public void encryptJsonString(String jsonString, File outputFile) throws Exception {
        SecretKey secretKey = getSecretKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
             CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher)) {
            fileOutputStream.write(iv); // Save IV for decryption
            cipherOutputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String decryptJsonString(File inputFile) throws Exception {
        SecretKey secretKey = getSecretKey();

        try (FileInputStream fileInputStream = new FileInputStream(inputFile)) {
            byte[] iv = new byte[12];
            int bytesRead = fileInputStream.read(iv);

            if (bytesRead != 12) {
                throw new IOException("Invalid IV length");
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            try (CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = cipherInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }

                byte[] decryptedData = byteArrayOutputStream.toByteArray();
                return new String(decryptedData, StandardCharsets.UTF_8);
            }
        }
    }
}

