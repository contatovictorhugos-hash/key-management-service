package com.key_management_service.service;

import com.key_management_service.dto.KeyResponseDTO;
import com.key_management_service.exception.KeyNotFoundException;
import com.key_management_service.model.CryptoKey;
import com.key_management_service.repository.CryptoKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KeyServiceTest {

    private CryptoKeyRepository cryptoKeyRepository;
    private CryptoService cryptoService;
    private KeyService keyService;

    // A valid 256-bit AES master key in Base64 (equivalent to e+jyHosAiRY/lwgEx+XyiPFcQhlRKRKI51iT4kBG4y8=)
    private static final String MASTER_KEY = "e+jyHosAiRY/lwgEx+XyiPFcQhlRKRKI51iT4kBG4y8=";

    @BeforeEach
    void setUp() {
        cryptoKeyRepository = Mockito.mock(CryptoKeyRepository.class);
        cryptoService = new CryptoService(MASTER_KEY);
        keyService = new KeyService(cryptoKeyRepository, cryptoService);
    }

    @Test
    void testDecryptPayload_Success() throws Exception {
        // 1. Generate RSA keypair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey rsaPublicKey = keyPair.getPublic();
        PrivateKey rsaPrivateKey = keyPair.getPrivate();

        String publicKeyPem = Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());
        String privateKeyPem = Base64.getEncoder().encodeToString(rsaPrivateKey.getEncoded());

        // 2. Encrypt the private key with the master key (simulating save behavior)
        String encryptedPrivateKeyDb = cryptoService.encryptPrivateKey(privateKeyPem);

        UUID keyId = UUID.randomUUID();
        CryptoKey cryptoKey = new CryptoKey(publicKeyPem, encryptedPrivateKeyDb);
        cryptoKey.setId(keyId);
        cryptoKey.setIsActive(true);

        when(cryptoKeyRepository.findById(keyId)).thenReturn(Optional.of(cryptoKey));

        // 3. Simulate caller encrypting payload (hybrid encryption)
        String originalText = "Hello, secret message!";
        
        // Generate symmetric AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        // Encrypt message with AES-CBC
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        byte[] encryptedContent = aesCipher.doFinal(originalText.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted content
        byte[] combinedAesOutput = new byte[iv.length + encryptedContent.length];
        System.arraycopy(iv, 0, combinedAesOutput, 0, iv.length);
        System.arraycopy(encryptedContent, 0, combinedAesOutput, iv.length, encryptedContent.length);
        String encryptedTextBase64 = Base64.getEncoder().encodeToString(combinedAesOutput);

        // Encrypt AES key with RSA public key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.getEncoded());
        String encryptedKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);

        // Construct the full payload
        String payload = keyId.toString() + "." + encryptedKeyBase64 + "." + encryptedTextBase64;

        // 4. Perform decryption
        String decryptedText = keyService.decryptPayload(payload);

        assertEquals(originalText, decryptedText);
    }

    @Test
    void testDecryptPayload_InactiveKey_ThrowsIllegalStateException() {
        UUID keyId = UUID.randomUUID();
        CryptoKey cryptoKey = new CryptoKey("dummyPublicKey", "dummyPrivateKey");
        cryptoKey.setId(keyId);
        cryptoKey.setIsActive(false); // Inactive key

        when(cryptoKeyRepository.findById(keyId)).thenReturn(Optional.of(cryptoKey));

        String payload = keyId.toString() + ".dummyKey.dummyText";

        assertThrows(IllegalStateException.class, () -> keyService.decryptPayload(payload));
    }

    @Test
    void testDecryptPayload_InvalidFormat_ThrowsIllegalArgumentException() {
        String payload = "invalid_format_without_three_parts";
        assertThrows(IllegalArgumentException.class, () -> keyService.decryptPayload(payload));
    }

    @Test
    void testDecryptPayload_KeyNotFound_ThrowsKeyNotFoundException() {
        UUID keyId = UUID.randomUUID();
        when(cryptoKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        String payload = keyId.toString() + ".dummyKey.dummyText";

        assertThrows(KeyNotFoundException.class, () -> keyService.decryptPayload(payload));
    }
}
