package com.project.backend.entity;

import java.time.Instant;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "otp",
    indexes = {
        @Index(name = "idx_identifier_active", columnList = "identifier, used"),
        @Index(name = "idx_created_at", columnList = "createdAt")
    }
)
@Getter @Setter
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private Instant expiryTime;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdAt;

    @Version // 🔥 handles concurrency
    private Long version;
}