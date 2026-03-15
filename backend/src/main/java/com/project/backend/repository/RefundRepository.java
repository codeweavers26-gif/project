package com.project.backend.repository;

import com.project.backend.entity.Refund;
import com.project.backend.entity.RefundStatus;
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
public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    // Fix 1: Use r.payment.id instead of r.transaction.id
    @Query("SELECT r FROM Refund r WHERE r.payment.id = :paymentId")
    List<Refund> findByPaymentId(@Param("paymentId") Long paymentId);
    
    // Alternative method name using Spring Data JPA convention
    List<Refund> findByPayment_Id(Long paymentId);
    
    // Fix 2: Find by return ID (if needed)
    Optional<Refund> findByReturnRequestId(Long returnId);
    
    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);
    
    // Fix 3: The main filters query with correct field name
    @Query("SELECT r FROM Refund r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:paymentId IS NULL OR r.payment.id = :paymentId) AND " +  // ← Changed from transactionId to paymentId
           "(:fromDate IS NULL OR r.createdAt >= :fromDate) AND " +    // ← Use createdAt, not requestedAt
           "(:toDate IS NULL OR r.createdAt <= :toDate)")
    Page<Refund> findByFilters(
            @Param("status") RefundStatus status,
            @Param("paymentId") Long paymentId,                       // ← Changed parameter name
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
    
    // Fix 4: Total refunded amount query with correct field
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status = 'PROCESSED'")
    BigDecimal getTotalRefundedAmount();
    
    // Optional: Find by transaction ID (the Razorpay transaction ID in your entity)
    Optional<Refund> findByTransactionId(String transactionId);
}