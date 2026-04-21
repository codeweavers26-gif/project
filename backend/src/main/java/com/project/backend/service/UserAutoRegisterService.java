package com.project.backend.service;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Finds an existing user by email or phone, or creates a new one.
     * Runs in its OWN transaction (REQUIRES_NEW) so a DataIntegrityViolationException
     * doesn't corrupt the outer Hibernate session.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User findOrCreate(String identifier) {

        // Always check first to avoid unnecessary save attempt
        return userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseGet(() -> {
                    try {
                        User user = new User();

                        if (identifier.contains("@")) {
                            user.setEmail(identifier);
                        } else {
                            user.setPhoneNumber(identifier);
                        }

                        user.setRole(Role.CUSTOMER);
                        user.setAuthProvider(AuthProvider.OTP);
                        user.setPassword(null);
                        user.setCreatedAt(Instant.now());

                        return userRepository.save(user);

                    } catch (DataIntegrityViolationException ex) {
                        // Race condition: another request created the user just now
                        log.warn("Race condition on auto-register for {}, fetching existing", identifier);
                        return userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                                .orElseThrow(() -> new RuntimeException("Failed to find or create user for: " + identifier));
                    }
                });
    }
}
