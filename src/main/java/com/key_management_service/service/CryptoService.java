package com.key_management_service.service;

import com.key_management_service.exception.CryptographyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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

    private String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
