package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Instant createdAt;
}
