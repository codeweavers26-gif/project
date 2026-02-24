package com.project.backend.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentTransaction payment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false, unique = true)
    private Return returnRequest;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 30)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}