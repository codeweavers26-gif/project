package com.project.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AuthResponse;
import com.project.backend.config.JwtUtils;
import com.project.backend.entity.AuthProvider;
import com.project.backend.entity.Otp;
import com.project.backend.entity.RefreshToken;
import com.project.backend.entity.Role;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.TooManyRequestsException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.repository.OtpRepository;
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
	private final OtpRepository otpRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SmsService smsService;
@Transactional
public AuthResponse register(RegisterRequest req) {

    String email = req.getEmail().toLowerCase().trim();
    String name = req.getName().trim();

    if (userRepository.findByEmail(email).isPresent()) {
        throw new BadRequestException("Email already registered");
    }

    User user = User.builder()
            .email(email)
            .password(passwordEncoder.encode(req.getPassword()))
            .name(name)
            .authProvider(AuthProvider.PASSWORD)
            .role(Role.CUSTOMER)
            .createdAt(Instant.now())
            .build();

    userRepository.save(user);

    String accessToken = jwtUtils.generateAccessToken(user);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    return new AuthResponse(accessToken, refreshToken.getToken());
}



	public AuthResponse login(AuthRequest req, Role expectedRole) {

    String email = req.getEmail().toLowerCase().trim();

    try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getPassword())
        );
    } catch (Exception ex) {
        throw new UnauthorizedException("Invalid email or password");
    }

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

if (user.getAuthProvider() != AuthProvider.PASSWORD) {
    throw new UnauthorizedException("Please login via OTP");
}
    if (user.getRole() != expectedRole) {
        throw new UnauthorizedException("Access denied");
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

		String accessToken = jwtUtils.generateAccessToken(user);

		return new AuthResponse(accessToken, rt.getToken());
	}

	@Transactional
	public void logout(String email) {

		userRepository.findByEmail(email).ifPresent(user -> {
			refreshTokenRepository.deleteByUser(user);
		});
	}
@Transactional
public void requestOtp(String rawIdentifier) {

    String identifier = normalize(rawIdentifier);

    long count = otpRepository.countByIdentifierAndCreatedAtAfter(
            identifier, Instant.now().minusSeconds(300));

    if (count >= 3) {
        throw new TooManyRequestsException("Too many OTP requests");
    }

    otpRepository.invalidateActiveOtps(identifier);

    String otp = otpService.generateOtp();
    String hash = passwordEncoder.encode(otp);

    Otp entity = new Otp();
    entity.setIdentifier(identifier);
    entity.setOtpHash(hash);
    entity.setExpiryTime(Instant.now().plusSeconds(300));
    entity.setAttempts(0);
    entity.setUsed(false);
    entity.setCreatedAt(Instant.now());

    otpRepository.save(entity);
 if (isEmail(identifier)) {
        emailService.sendOtp(identifier, otp);
    } else {
        smsService.sendOtp(identifier, otp);
    }
  //  notificationService.sendOtp(identifier, otp);
}

@Transactional
public AuthResponse verifyOtp(String rawIdentifier,
                              String otpInput,
                              String ip,
                              String userAgent) {

    String identifier = normalize(rawIdentifier);

    Otp otp = otpRepository.findActiveOtpForUpdate(identifier)
            .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

    if (otp.isUsed()) {
        throw new BadRequestException("OTP already used");
    }

    if (Instant.now().isAfter(otp.getExpiryTime())) {
        otp.setUsed(true);
        otpRepository.save(otp); // ✅ ensure persist
        throw new BadRequestException("OTP expired");
    }

    if (otp.getAttempts() >= 5) {
        otp.setUsed(true);
        otpRepository.save(otp); 
        throw new BadRequestException("Too many attempts");
    }

    if (!passwordEncoder.matches(otpInput, otp.getOtpHash())) {
        otp.setAttempts(otp.getAttempts() + 1);
        otpRepository.save(otp); 
        throw new BadRequestException("Invalid OTP");
    }

    otp.setUsed(true);
    otpRepository.save(otp);

    User user = userRepository.findByEmailOrPhoneNumber(identifier,(identifier))
            .orElseGet(() -> safeAutoRegister(identifier));

    return generateAuth(user, ip, userAgent);
}
private User safeAutoRegister(String identifier) {

    try {
        User user = new User();

        if (isEmail(identifier)) {
            user.setEmail(identifier);
        } else {
            user.setPhoneNumber((identifier));
        }

        user.setRole(Role.CUSTOMER);
        user.setAuthProvider(AuthProvider.OTP) ;
        user.setPassword(null);
        user.setCreatedAt(Instant.now());

        return userRepository.save(user);

    } catch (DataIntegrityViolationException ex) {
        return userRepository.findByEmailOrPhoneNumber(identifier,(identifier))
                .orElseThrow();
    }
}




private String normalize(String input) {
    input = input.trim();

    if (isEmail(input)) {
        return input.toLowerCase();
    }

    return input.replaceAll("[^0-9]", "");
}

private boolean isEmail(String input) {
    return input.contains("@");
}

@Transactional
public AuthResponse generateAuth(User user, String ip, String userAgent) {

    List<RefreshToken> tokens =
            refreshTokenRepository.findByUserOrderByCreatedAtDesc(user);

    if (tokens.size() >= 3) {
        List<RefreshToken> toDelete = tokens.subList(2, tokens.size());
        refreshTokenRepository.deleteAll(toDelete);
    }
    String accessToken = jwtUtils.generateAccessToken(user);

    RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
           .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
            .createdAt(Instant.now())
         //   .ip(ip)
           // .userAgent(userAgent)
            .build();

    refreshTokenRepository.save(refreshToken);

    return new AuthResponse(
            accessToken,
            refreshToken.getToken()
    );
}

}