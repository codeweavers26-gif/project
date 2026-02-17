package com.project.backend.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Order;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findAll(Pageable pageable);

	Page<Order> findByUser(User user, Pageable pageable);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

	Page<Order> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

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
	Long countByCreatedAtAfter(Instant time);
	Long countByStatus(OrderStatus status);

	Long countByPaymentMethod(PaymentMethod cod);

	@Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.createdAt >= :time")
	Double sumTotalAmountByCreatedAtAfter(Instant time);

	@Query("SELECT COALESCE(SUM(o.taxAmount),0) FROM Order o WHERE o.createdAt >= :time")
	Double sumTaxAmountByCreatedAtAfter(Instant time);
	
	@Query("""
		    SELECT o FROM Order o
		    WHERE (:status IS NULL OR o.status = :status)
		      AND (:userId IS NULL OR o.user.id = :userId)
		      AND (:orderId IS NULL OR o.id = :orderId)
		      AND (:email IS NULL OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :email, '%')))
		      AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
		      AND (:toDate IS NULL OR o.createdAt <= :toDate)
		""")
		Page<Order> searchOrders(
		        OrderStatus status,
		        Long userId,
		        Long orderId,
		        String email,
		        Instant fromDate,
		        Instant toDate,
		        Pageable pageable);


	 @Query("SELECT DISTINCT o FROM Order o " +
	           "LEFT JOIN o.items i " +
	           "LEFT JOIN i.product p " +
	           "WHERE (:userId IS NULL OR o.user.id = :userId) " +
	           "AND (:status IS NULL OR o.status = :status) " +
	           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
	           "AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod) " +
	           "AND (:minAmount IS NULL OR o.totalAmount >= :minAmount) " +
	           "AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount) " +
	           "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
	           "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
	           "AND (:search IS NULL OR " +
	           "    CAST(o.id AS string) LIKE %:search% OR " +
	           "    p.name LIKE %:search% OR " +
	           "    p.brand LIKE %:search%)")
	    Page<Order> findOrdersByFilters(
	            @Param("userId") Long userId,
	            @Param("status") OrderStatus status,
	            @Param("paymentStatus") PaymentStatus paymentStatus,
	            @Param("paymentMethod") PaymentMethod paymentMethod,
	            @Param("minAmount") Double minAmount,
	            @Param("maxAmount") Double maxAmount,
	            @Param("fromDate") Instant fromDate,
	            @Param("toDate") Instant toDate,
	            @Param("search") String search,
	            Pageable pageable);
	
}
