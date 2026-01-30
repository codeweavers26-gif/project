package com.project.backend.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.entity.Role;
import com.project.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Long countByRole(Role role);
    Long countByCreatedAtAfter(Instant time);

}
