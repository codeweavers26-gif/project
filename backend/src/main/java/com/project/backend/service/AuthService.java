package com.project.backend.service;

import java.time.Instant;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AuthResponse;
import com.project.backend.config.JwtUtils;
import com.project.backend.entity.RefreshToken;
import com.project.backend.entity.Role;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
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

		User user = User.builder().email(req.getEmail()).password(passwordEncoder.encode(req.getPassword()))
				.name(req.getName()).role(Role.CUSTOMER).createdAt(Instant.now()) // customer always
				.build();

		userRepository.save(user);

		// 🔥 PASS USER OBJECT
		String accessToken = jwtUtils.generateAccessToken(user);

		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

		return new AuthResponse(accessToken, refreshToken.getToken());
	}

	public AuthResponse login(AuthRequest req, Role expectedRole) {

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

		User user = userRepository.findByEmail(req.getEmail())
				.orElseThrow(() -> new NotFoundException("User not found"));

		// 🔥 Portal restriction happens HERE
		if (user.getRole() != expectedRole) {
			throw new UnauthorizedException("Access denied for this portal");
		}

		String accessToken = jwtUtils.generateAccessToken(user);
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

		return new AuthResponse(accessToken, refreshToken.getToken());
	}

	public AuthResponse refreshAccessToken(String refreshTokenStr) {

		RefreshToken rt = refreshTokenRepository.findByToken(refreshTokenStr)
				.orElseThrow(() -> new RuntimeException("Invalid refresh token"));

		if (refreshTokenService.isExpired(rt)) {
			throw new RuntimeException("Refresh token expired. Please login again.");
		}

		User user = rt.getUser();

		// ✅ PASS FULL USER
		String accessToken = jwtUtils.generateAccessToken(user);

		return new AuthResponse(accessToken, rt.getToken());
	}

	@Transactional
	public void logout(String email) {

		userRepository.findByEmail(email).ifPresent(user -> {
			refreshTokenRepository.deleteByUser(user);
		});
	}

}