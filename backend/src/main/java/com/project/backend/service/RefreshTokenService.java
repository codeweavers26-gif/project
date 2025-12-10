package com.project.backend.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.project.backend.entity.RefreshToken;
import com.project.backend.entity.User;
import com.project.backend.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refreshTokenExpirationMs}")
    private long refreshExpiryMs;

    // ✔ THIS METHOD MUST EXIST
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpiryMs))
                .build();

        return refreshTokenRepository.save(token);
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiresAt().isBefore(Instant.now());
    }

    public void deleteTokensByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
