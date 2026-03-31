package com.project.backend.service;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.backend.entity.Otp;
import com.project.backend.exception.TooManyRequestsException;
import com.project.backend.repository.OtpRepository;
import com.project.backend.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    private static final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }

    @Transactional
    public void requestOtp(String rawIdentifier, String ip, String userAgent) {

        String identifier = normalize(rawIdentifier);

        long count = otpRepository.countByIdentifierAndCreatedAtAfter(
                identifier, Instant.now().minusSeconds(300));

        if (count >= 3) {
            throw new TooManyRequestsException("Too many OTP requests");
        }

        otpRepository.invalidateActiveOtps(identifier);

        String otp = generateOtp();
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
}