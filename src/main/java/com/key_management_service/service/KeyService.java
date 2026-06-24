package com.key_management_service.service;

import com.key_management_service.dto.KeyResponseDTO;
import com.key_management_service.exception.KeyGenerationException;
import com.key_management_service.exception.KeyNotFoundException;
import com.key_management_service.model.CryptoKey;
import com.key_management_service.repository.CryptoKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.UUID;

@Service
public class KeyService {

    private final CryptoKeyRepository cryptoKeyRepository;
    private final CryptoService cryptoService;

    public KeyService(CryptoKeyRepository cryptoKeyRepository, CryptoService cryptoService) {
        this.cryptoKeyRepository = cryptoKeyRepository;
        this.cryptoService = cryptoService;
    }

    public KeyResponseDTO generateKey() {

        try {
            KeyPair keyPair = generateRsaKeyPair();

            String publicKey = encodeToBase64(keyPair.getPublic().getEncoded());
            String privateKey = encodeToBase64(keyPair.getPrivate().getEncoded());

            String encryptedPrivateKey = cryptoService.encryptPrivateKey(privateKey);

            CryptoKey cryptoKey = new CryptoKey(publicKey, encryptedPrivateKey);

            CryptoKey savedCryptoKey = saveCryptoKey(cryptoKey);

            return new KeyResponseDTO(
                    savedCryptoKey.getId(),
                    savedCryptoKey.getPublicKey());

        } catch (GeneralSecurityException exception) {
            throw new KeyGenerationException("Error generating key pair", exception);
        }
    }

    private CryptoKey saveCryptoKey(CryptoKey cryptoKey) {
        return cryptoKeyRepository.save(cryptoKey);
    }

    private CryptoKey findCryptoKey(UUID id) {
        return cryptoKeyRepository.findById(id)
                .orElseThrow(() -> new KeyNotFoundException("Key not found with ID: " + id));
    }

    private KeyPair generateRsaKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String decryptPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be empty");
        }

        String[] parts = payload.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid payload format. Expected format: KeyId.EncriptKey.EncriptText");
        }

        UUID keyId;
        try {
            keyId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid KeyId format in payload", e);
        }

        String encryptedKeyBase64 = parts[1];
        String encryptedTextBase64 = parts[2];

        CryptoKey cryptoKey = findCryptoKey(keyId);
        if (!Boolean.TRUE.equals(cryptoKey.getIsActive())) {
            throw new IllegalStateException("The requested key is deactivated");
        }

        String privateKeyPem = cryptoService.decryptPrivateKey(cryptoKey.getPrivateKey());

        PrivateKey rsaPrivateKey;
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyPem);
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(
                    privateKeyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            rsaPrivateKey = kf.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new com.key_management_service.exception.CryptographyException("Error reconstructing private key", e);
        }

        byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(encryptedKeyBase64);
        byte[] aesKeyBytes = cryptoService.decryptRsa(encryptedAesKeyBytes, rsaPrivateKey);

        byte[] combinedAesOutput = Base64.getDecoder().decode(encryptedTextBase64);
        return cryptoService.decryptAes(combinedAesOutput, aesKeyBytes);
    }

    @Transactional
    public void deactivateKey(UUID id) {
        CryptoKey cryptoKey = findCryptoKey(id);
        cryptoKey.setIsActive(false);
        saveCryptoKey(cryptoKey);
    }
}
