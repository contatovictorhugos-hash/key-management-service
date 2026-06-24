package com.key_management_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crypto_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @Column(columnDefinition = "TEXT")
    private String privateKey;

    private Boolean isActive;

    private LocalDateTime createdAt;

    public CryptoKey(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }
}
