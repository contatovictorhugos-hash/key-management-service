package com.key_management_service.dto;

import jakarta.validation.constraints.NotBlank;

public record DecryptRequestDTO(
        @NotBlank(message = "O payload não pode estar vazio")
        String payload
) {}
