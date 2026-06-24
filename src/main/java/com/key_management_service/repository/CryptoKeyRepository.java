package com.key_management_service.repository;

import com.key_management_service.model.CryptoKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CryptoKeyRepository extends JpaRepository<CryptoKey, UUID> {
}
