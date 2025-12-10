package com.project.backend.requestDto;


import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}