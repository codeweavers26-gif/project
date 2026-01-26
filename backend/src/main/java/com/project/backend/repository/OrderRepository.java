package com.project.backend.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Order;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findAll(Pageable pageable);

	Page<Order> findByUser(User user, Pageable pageable);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

	Page<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
	Page<Order> findByUserEmailContainingIgnoreCase(
	        String email,
	        Pageable pageable);

	// For orderId search with pagination
	@Query("SELECT o FROM Order o WHERE o.id = :orderId")
	Page<Order> findById(@Param("orderId") Long orderId, Pageable pageable);
	
	@Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED'")
	Double getTotalSales();

	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
	Long countDeliveredOrders();

	@Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE o.status = 'DELIVERED'")
	Long getTotalItemsSold();


}
