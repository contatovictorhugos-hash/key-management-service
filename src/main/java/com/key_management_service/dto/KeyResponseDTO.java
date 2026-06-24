package com.key_management_service.dto;

import java.util.UUID;

public record KeyResponseDTO(
        UUID keyId,
        String publicKey) {
}