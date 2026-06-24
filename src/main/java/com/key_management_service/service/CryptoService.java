package com.key_management_service.service;

import com.key_management_service.exception.CryptographyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private final SecretKeySpec masterKey;

    public CryptoService(@Value("${app.security.master-key}") String masterKey) {
        try {
            this.masterKey = new SecretKeySpec(
                    Base64.getDecoder().decode(masterKey),
                    "AES"
            );
        } catch (IllegalArgumentException exception) {
            throw new CryptographyException("Invalid master key configuration", exception);
        }
    }

    public String encryptPrivateKey(String privateKey) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

            byte[] encryptedPrivateKey = cipher.doFinal(privateKey.getBytes(StandardCharsets.UTF_8));

            return encodeToBase64(iv) + ":" + encodeToBase64(encryptedPrivateKey);
        } catch (GeneralSecurityException exception) {
            throw new CryptographyException("Error encrypting private key", exception);
        }
    }

    public String decryptPrivateKey(String encryptedPrivateKey) {
        try {
            String[] parts = encryptedPrivateKey.split(":");
            if (parts.length != 2) {
                throw new CryptographyException("Invalid encrypted private key format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

            byte[] decryptedPrivateKey = cipher.doFinal(encryptedData);

            return new String(decryptedPrivateKey, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new CryptographyException("Error decrypting private key", exception);
        }
    }

    public byte[] decryptRsa(byte[] encryptedAesKeyBytes, PrivateKey privateKey) {
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            return rsaCipher.doFinal(encryptedAesKeyBytes);
        } catch (GeneralSecurityException exception) {
            throw new CryptographyException("Error decrypting AES key with RSA private key", exception);
        }
    }

    public String decryptAes(byte[] combinedAesOutput, byte[] aesKeyBytes) {
        try {
            if (combinedAesOutput.length < 16) {
                throw new CryptographyException("Invalid encrypted payload size");
            }

            byte[] iv = new byte[16];
            byte[] encryptedContentBytes = new byte[combinedAesOutput.length - 16];

            System.arraycopy(combinedAesOutput, 0, iv, 0, 16);
            System.arraycopy(combinedAesOutput, 16, encryptedContentBytes, 0, encryptedContentBytes.length);

            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

            byte[] decryptedBytes = aesCipher.doFinal(encryptedContentBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new CryptographyException("Error decrypting content with AES key", exception);
        }
    }

    private String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
