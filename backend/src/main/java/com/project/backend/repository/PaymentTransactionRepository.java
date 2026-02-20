package com.project.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
    
    Optional<PaymentTransaction> findByOrderId(Long orderId);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.order.id = :orderId AND pt.status = 'PAID'")
    Optional<PaymentTransaction> findSuccessfulPaymentByOrderId(@Param("orderId") Long orderId);
    
    @Modifying
    @Transactional
    @Query("UPDATE PaymentTransaction pt SET pt.status = :status, pt.completedAt = CURRENT_TIMESTAMP WHERE pt.razorpayOrderId = :razorpayOrderId")
    int updateStatusByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId, @Param("status") String status);
}