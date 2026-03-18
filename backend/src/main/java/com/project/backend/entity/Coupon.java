package com.project.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "code", unique = true),
    @Index(name = "idx_coupon_status", columnList = "status"),
    @Index(name = "idx_validity", columnList = "valid_from, valid_to")
})
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue; 

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount; 

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(nullable = false)
    private Integer usageLimit; 

    @Column(nullable = false)
    private Integer usagePerUser; 

    @Column(nullable = false)
    private Integer totalUsedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status; 

    @ElementCollection
    @CollectionTable(name = "coupon_applicable_categories", 
                     joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "category_id")
    private Set<Long> applicableCategoryIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "coupon_applicable_products", 
                     joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "product_id")
    private Set<Long> applicableProductIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "coupon_excluded_categories", 
                     joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "category_id")
    private Set<Long> excludedCategoryIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "coupon_excluded_products", 
                     joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "product_id")
    private Set<Long> excludedProductIds = new HashSet<>();

    @Column(name = "is_first_order_only")
    private Boolean isFirstOrderOnly = false;

    @Column(name = "is_new_user_only")
    private Boolean isNewUserOnly = false;

    @Column(name = "applicable_payment_methods")
    private String applicablePaymentMethods; 

    @Column(name = "applicable_user_tiers")
    private String applicableUserTiers; 

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Version
    private Long version; 

    @PrePersist
    @PreUpdate
    private void validateCoupon() {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("Valid from date must be before valid to date");
        }
        
        if (type == CouponType.PERCENTAGE && (discountValue.compareTo(BigDecimal.ZERO) <= 0 || 
            discountValue.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Percentage discount must be between 0 and 100");
        }
        
        if (type == CouponType.FIXED && discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fixed discount must be greater than 0");
        }
        
        if (usageLimit < 0 || usagePerUser < 0) {
            throw new IllegalArgumentException("Usage limits cannot be negative");
        }
    }
}