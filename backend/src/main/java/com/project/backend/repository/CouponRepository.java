package com.project.backend.repository;

import com.project.backend.entity.Coupon;
import com.project.backend.entity.CouponStatus;
import com.project.backend.entity.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    Page<Coupon> findByStatus(CouponStatus status, Pageable pageable);

    List<Coupon> findByValidToBeforeAndStatus(LocalDateTime date, CouponStatus status);

    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validTo >= :now")
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE " +
           "(:search IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:fromDate IS NULL OR c.validFrom >= :fromDate) AND " +
           "(:toDate IS NULL OR c.validTo <= :toDate)")
    Page<Coupon> findByFilters(
            @Param("search") String search,
            @Param("type") CouponType type,
            @Param("status") CouponStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT SUM(u.discountAmount) FROM CouponUsage u WHERE u.coupon.id = :couponId AND u.wasSuccessful = true")
    BigDecimal getTotalDiscountByCoupon(@Param("couponId") Long couponId);

    @Modifying
    @Query("UPDATE Coupon c SET c.status = 'EXPIRED' WHERE c.validTo < :now AND c.status = 'ACTIVE'")
    int expireCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c.type, COUNT(c) FROM Coupon c GROUP BY c.type")
    List<Object[]> countByType();

    @Query("SELECT c.status, COUNT(c) FROM Coupon c GROUP BY c.status")
    List<Object[]> countByStatus();
}