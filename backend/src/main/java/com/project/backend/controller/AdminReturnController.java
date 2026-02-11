package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AdminUserReturnResponseDto;
import com.project.backend.ResponseDto.ReturnResponseDto;
import com.project.backend.entity.ReturnStatus;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.UpdateReturnStatusDto;
import com.project.backend.service.AdminReturnService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Returns")
public class AdminReturnController {

	private final AdminReturnService service;

	@Operation(summary = "Get all returns", security = @SecurityRequirement(name = "Bearer Authentication"))
	@GetMapping
	public ResponseEntity<?> getAll(@RequestParam int page, @RequestParam int size) {

		return ResponseEntity.ok(service.getAllReturns(page, size));
	}

	@Operation(summary = "Update return status", security = @SecurityRequirement(name = "Bearer Authentication"))
	@PutMapping("/{returnId}")
	public ResponseEntity<Void> update(@PathVariable Long returnId, @RequestBody UpdateReturnStatusDto dto) {

		service.updateReturnStatus(returnId, dto);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get all returns of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
	@GetMapping("/user/{userId}")
	public ResponseEntity<PageResponseDto<AdminUserReturnResponseDto>> getUserReturns(@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(service.getReturnsByUser(userId, page, size));
	}

}
