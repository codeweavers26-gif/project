package com.project.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Otp;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;

@Repository
public interface OtpRepository  extends JpaRepository<Otp, Long>{
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT o FROM Otp o
    WHERE o.identifier = :identifier
    AND o.used = false
    ORDER BY o.createdAt DESC
""")
Optional<Otp> findActiveOtpForUpdate(String identifier);


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
