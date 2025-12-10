package com.project.backend.service;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AuthResponse;
import com.project.backend.config.JwtUtils;
import com.project.backend.entity.RefreshToken;
import com.project.backend.entity.Role;
import com.project.backend.entity.User;
import com.project.backend.repository.RefreshTokenRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.AuthRequest;
import com.project.backend.requestDto.RegisterRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already taken");
        }
        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse login(AuthRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
        String accessToken = jwtUtils.generateAccessToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refreshAccessToken(String refreshTokenStr) {
        RefreshToken rt = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (refreshTokenService.isExpired(rt)) {
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        String accessToken = jwtUtils.generateAccessToken(rt.getUser().getEmail());
        return new AuthResponse(accessToken, rt.getToken());
    }

    public void logout(User user) {
    	refreshTokenRepository.deleteByUser(user);
    }
}