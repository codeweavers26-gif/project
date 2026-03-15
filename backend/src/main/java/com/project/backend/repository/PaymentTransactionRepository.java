package com.project.backend.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
     Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
    
    Optional<PaymentTransaction> findByOrderId(Long orderId);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.order.id = :orderId AND pt.status = 'PAID'")
    Optional<PaymentTransaction> findSuccessfulPaymentByOrderId(@Param("orderId") Long orderId);
    
    @Modifying
    @Transactional
    @Query("UPDATE PaymentTransaction pt SET pt.status = :status, pt.completedAt = CURRENT_TIMESTAMP WHERE pt.razorpayOrderId = :razorpayOrderId")
    int updateStatusByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId, @Param("status") String status);


        @Query("SELECT p FROM PaymentTransaction p WHERE " +
           "(:search IS NULL OR " +
           "LOWER(p.razorpayOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(p.order.id AS string) LIKE CONCAT('%', :search, '%')) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:paymentMethod IS NULL OR p.razorpayPaymentMethod = :paymentMethod) AND " +
           "(:userId IS NULL OR p.order.user.id = :userId) AND " +
           "(:orderId IS NULL OR p.order.id = :orderId) AND " +
           "(:fromDate IS NULL OR p.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR p.createdAt <= :toDate) AND " +
           "(:minAmount IS NULL OR p.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR p.amount <= :maxAmount)")
    Page<PaymentTransaction> findByFilters(
            @Param("search") String search,
            @Param("status") PaymentStatus status,
            @Param("paymentMethod") String paymentMethod,
            @Param("userId") Long userId,
            @Param("orderId") Long orderId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
 @Query("SELECT SUM(p.amount) FROM PaymentTransaction p WHERE p.status = 'SUCCESS' AND p.completedAt >= :startDate")
    BigDecimal getRevenueSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT p.razorpayPaymentMethod as method, COUNT(p) as count, SUM(p.amount) as total " +
           "FROM PaymentTransaction p WHERE p.status = 'SUCCESS' " +
           "GROUP BY p.razorpayPaymentMethod")
    List<Object[]> getPaymentMethodStats();
    
    @Query("SELECT p.status as status, COUNT(p) as count, SUM(p.amount) as total " +
           "FROM PaymentTransaction p GROUP BY p.status")
    List<Object[]> getPaymentStatusStats();
    
    @Query("SELECT DATE(p.completedAt) as date, COUNT(p) as count, SUM(p.amount) as amount " +
           "FROM PaymentTransaction p WHERE p.status = 'SUCCESS' " +
           "AND p.completedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(p.completedAt) ORDER BY date")
    List<Object[]> getDailyRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT HOUR(p.completedAt) as hour, COUNT(p) as count, SUM(p.amount) as amount " +
           "FROM PaymentTransaction p WHERE p.status = 'SUCCESS' " +
           "AND DATE(p.completedAt) = DATE(:date) " +
           "GROUP BY HOUR(p.completedAt) ORDER BY hour")
    List<Object[]> getHourlyRevenue(@Param("date") LocalDateTime date);

               @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.status = :status")
    Long countByStatus(@Param("status") PaymentStatus status);
    
    @Query("SELECT SUM(p.amount) FROM PaymentTransaction p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT SUM(p.amount) FROM PaymentTransaction p WHERE p.status = 'SUCCESS' AND DATE(p.completedAt) = CURRENT_DATE")
    BigDecimal getTodayRevenue();
    
}