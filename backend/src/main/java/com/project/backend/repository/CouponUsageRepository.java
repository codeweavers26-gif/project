package com.project.backend.repository;

import com.project.backend.entity.CouponUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
Page<CouponUsage> findByCouponId(Long couponId, Pageable pageable);
    Long countByCouponIdAndUserId(Long couponId, Long userId);

    Long countByCouponId(Long couponId);

    boolean existsByCouponIdAndUserIdAndOrderId(Long couponId, Long userId, Long orderId);

    List<CouponUsage> findByCouponId(Long couponId);

    Page<CouponUsage> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(u.discountAmount), 0) FROM CouponUsage u WHERE u.user.id = :userId AND u.wasSuccessful = true")
    BigDecimal getTotalDiscountByUser(@Param("userId") Long userId);

    @Query("SELECT u FROM CouponUsage u WHERE " +
           "(:couponId IS NULL OR u.coupon.id = :couponId) AND " +
           "(:userId IS NULL OR u.user.id = :userId) AND " +
           "(:fromDate IS NULL OR u.usedAt >= :fromDate) AND " +
           "(:toDate IS NULL OR u.usedAt <= :toDate)")
    Page<CouponUsage> findByFilters(
            @Param("couponId") Long couponId,
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT DATE(u.usedAt) as date, COUNT(u) as count, SUM(u.discountAmount) as total " +
           "FROM CouponUsage u WHERE u.usedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(u.usedAt) ORDER BY date")
    List<Object[]> getDailyUsageStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Optional<CouponUsage> findByOrderId(Long orderId);
}
