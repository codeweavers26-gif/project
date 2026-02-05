package com.project.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Role;
import com.project.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Long countByRole(Role role);
    Long countByCreatedAtAfter(Instant time);
    @Query("""
    	    SELECT u FROM User u
    	    WHERE (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
    	           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
    	      AND (:from IS NULL OR u.createdAt >= :from)
    	      AND (:to IS NULL OR u.createdAt <= :to)
    	""")
    	Page<User> searchUsers(String search, Instant from, Instant to, Pageable pageable);




}
