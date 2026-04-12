package com.project.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Otp;

import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM Otp o
        WHERE o.identifier = :identifier
        AND o.used = false
        ORDER BY o.createdAt DESC
    """)
    List<Otp> findActiveOtpsForUpdate(@Param("identifier") String identifier);

    default Optional<Otp> findActiveOtpForUpdate(String identifier) {
        List<Otp> otps = findActiveOtpsForUpdate(identifier);
        return otps.isEmpty() ? Optional.empty() : Optional.of(otps.get(0));
    }

    long countByIdentifierAndCreatedAtAfter(String identifier, Instant time);

    @Modifying
    @Query("""
        UPDATE Otp o
        SET o.used = true
        WHERE o.identifier = :identifier
        AND o.used = false
    """)
    void invalidateActiveOtps(@Param("identifier") String identifier);

}
