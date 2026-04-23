package com.project.backend.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.AuthProvider;
import com.project.backend.entity.Role;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAutoRegisterService {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User findOrCreate(String identifier) {

        // Always check first — no try/catch, no dirty session
        Optional<User> existing = userRepository.findByEmailOrPhoneNumber(identifier, identifier);
        if (existing.isPresent()) {
            log.info("Found existing user for identifier {}", identifier);
            return existing.get();
        }

        User user = new User();
        if (identifier.contains("@")) {
            user.setEmail(identifier);
        } else {
            user.setPhoneNumber(identifier);
            // email stays null — requires ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL
        }

        user.setRole(Role.CUSTOMER);
        user.setAuthProvider(AuthProvider.OTP);
        user.setPassword(null);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        log.info("Auto-registered new user id={} for identifier {}", saved.getId(), identifier);
        return saved;
    }
}
