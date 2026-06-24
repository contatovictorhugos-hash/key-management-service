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
                    savedCryptoKey.getPublicKey()
            );

        } catch (GeneralSecurityException exception) {
            throw new KeyGenerationException("Error generating key pair", exception);
        }
    }

    private CryptoKey saveCryptoKey(CryptoKey cryptoKey){
        return cryptoKeyRepository.save(cryptoKey);
    }

    private CryptoKey findCryptoKey(UUID id){
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

    @Transactional
    public void deactivateKey(UUID id) {
        CryptoKey cryptoKey = findCryptoKey(id);
        cryptoKey.setIsActive(false);
        saveCryptoKey(cryptoKey);
    }
}
