package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.requestDto.InventoryRequestDto;
import com.project.backend.service.InventoryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin - Inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<Void> upsertInventory(
            @Validated @RequestBody InventoryRequestDto dto) {

        inventoryService.upsertInventory(dto);
        return ResponseEntity.ok().build();
    }
}
