package com.project.backend.requestDto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
}