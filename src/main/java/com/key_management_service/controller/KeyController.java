package com.key_management_service.controller;

import com.key_management_service.dto.DecryptRequestDTO;
import com.key_management_service.dto.DecryptResponseDTO;
import com.key_management_service.dto.KeyResponseDTO;
import com.key_management_service.service.KeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/keys")
public class KeyController {

    private final KeyService keyService;

    public KeyController(KeyService keyService) {
        this.keyService = keyService;
    }

    @PostMapping
    public ResponseEntity<KeyResponseDTO> getKeys() {
        KeyResponseDTO keyResponseDTO = keyService.generateKey();
        return ResponseEntity.status(HttpStatus.CREATED).body(keyResponseDTO);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateKey(@PathVariable UUID id) {
        keyService.deactivateKey(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/decrypt")
    public ResponseEntity<DecryptResponseDTO> decrypt(@Valid @RequestBody DecryptRequestDTO dto) {
        String decryptedText = keyService.decryptPayload(dto.payload());
        return ResponseEntity.ok(new DecryptResponseDTO(decryptedText));
    }
}

