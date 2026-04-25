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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Stored as comma-separated IDs ("1,2,3") in a single column.
    // Avoids @ElementCollection separate tables and lazy-loading issues.
    // Use getApplicableCategoryIds() / setApplicableCategoryIds() for Set<Long> access.

    @Column(name = "applicable_category_ids", length = 500)
    private String applicableCategoryIdsStr;

    @Column(name = "applicable_product_ids", length = 500)
    private String applicableProductIdsStr;

    @Column(name = "excluded_category_ids", length = 500)
    private String excludedCategoryIdsStr;

    @Column(name = "excluded_product_ids", length = 500)
    private String excludedProductIdsStr;

    // ── Convenience accessors (not persisted — JPA uses field access) ────────

    public Set<Long> getApplicableCategoryIds() {
        return parseCsv(applicableCategoryIdsStr);
    }

    public void setApplicableCategoryIds(Set<Long> ids) {
        this.applicableCategoryIdsStr = toCsv(ids);
    }

    public Set<Long> getApplicableProductIds() {
        return parseCsv(applicableProductIdsStr);
    }

    public void setApplicableProductIds(Set<Long> ids) {
        this.applicableProductIdsStr = toCsv(ids);
    }

    public Set<Long> getExcludedCategoryIds() {
        return parseCsv(excludedCategoryIdsStr);
    }

    public void setExcludedCategoryIds(Set<Long> ids) {
        this.excludedCategoryIdsStr = toCsv(ids);
    }

    public Set<Long> getExcludedProductIds() {
        return parseCsv(excludedProductIdsStr);
    }

    public void setExcludedProductIds(Set<Long> ids) {
        this.excludedProductIdsStr = toCsv(ids);
    }

    private static Set<Long> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return new HashSet<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String toCsv(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

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