package com.project.backend.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;  // Link to your existing Order
    
    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;
    
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;
    
    @Column(name = "razorpay_signature")
    private String razorpaySignature;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 3)
    private String currency = "INR";
    
    @Column(length = 20)
    private String status; // CREATED, ATTEMPTED, PAID, FAILED
    
    @Column(name = "payment_method", length = 30)
    private String razorpayPaymentMethod; // card, upi, netbanking

    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}