package com.project.backend.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "returns")
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(unique = true, nullable = false)
    private String returnNumber; 
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private ReturnReason reason;
    
    @Column(columnDefinition = "TEXT")
    private String reasonDescription;  
    
    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ReturnStatus status;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;  
    
    @Column(precision = 10, scale = 2)
    private BigDecimal restockingFee;  
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalRefundAmount; 
    
    @Column(precision = 10, scale = 2)
    private BigDecimal shippingCost; 

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "approved_by")
    private Long approvedBy;  
    
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "pickup_scheduled_date")
    private LocalDateTime pickupScheduledDate;
    
    @Column(name = "pickup_address")
    private String pickupAddress;
    
    @Column(name = "pickup_contact")
    private String pickupContact;
    
    @Column(name = "pickup_agent")
    private String pickupAgent;
    
    @Column(name = "pickup_tracking_number")
    private String pickupTrackingNumber;
    
    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "qc_passed")
    private Boolean qcPassed;
    
    @Column(name = "qc_checked_by")
    private String qcCheckedBy;
    
    @Column(name = "qc_checked_at")
    private LocalDateTime qcCheckedAt;
    
    @Column(name = "qc_comments")
    private String qcComments;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Refund refund;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        returnNumber = generateReturnNumber();
        
        if (status == null) {
            status = ReturnStatus.PENDING_APPROVAL;
        }
        
        if (refundAmount == null) {
            refundAmount = BigDecimal.ZERO;
        }
        if (restockingFee == null) {
            restockingFee = BigDecimal.ZERO;
        }
        if (totalRefundAmount == null) {
            totalRefundAmount = BigDecimal.ZERO;
        }
        if (shippingCost == null) {
            shippingCost = BigDecimal.ZERO;
        }
    }	

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateReturnNumber() {
        return "RTV-" + java.time.Year.now().getValue() + "-" + 
               String.format("%06d", System.currentTimeMillis() % 1000000);
    }
}