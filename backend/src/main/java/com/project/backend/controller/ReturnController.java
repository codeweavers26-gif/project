package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.ReturnResponseDto;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.ReturnRequestDto;
import com.project.backend.service.AdminReturnService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Tag(name = "Customer - Returns")
public class ReturnController {

    private final AdminReturnService returnService;

    private final UserRepository userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    @Operation(summary = "Request product return",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ReturnResponseDto> requestReturn(
            Authentication auth,
            @RequestBody ReturnRequestDto dto) {

        return ResponseEntity.ok(
            returnService.requestReturn(getUser(auth), dto)
        );
    }
}
