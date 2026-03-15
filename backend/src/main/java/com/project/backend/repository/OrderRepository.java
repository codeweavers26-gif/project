package com.project.backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

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
Long countByUserId(Long userId);
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
	
	   @Query("SELECT DISTINCT o FROM Order o " +
	           "LEFT JOIN FETCH o.user u " +
	           "LEFT JOIN FETCH o.items i " +
	           "WHERE (:status IS NULL OR o.status = :status) " +
	           "AND (:userId IS NULL OR o.user.id = :userId) " +
	           "AND (:orderId IS NULL OR o.id = :orderId) " +
	           "AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
	           "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
	           "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
	    Page<Order> searchOrders(
	            @Param("status") OrderStatus status,
	            @Param("userId") Long userId,
	            @Param("orderId") Long orderId,
	            @Param("email") String email,
	            @Param("fromDate") Instant fromDate,
	            @Param("toDate") Instant toDate,
	            Pageable pageable);
	   
	   @Query("SELECT DISTINCT o FROM Order o " +
		       "LEFT JOIN FETCH o.user u " +
		       "WHERE (:userId IS NULL OR o.user.id = :userId) " +
		       "AND (:status IS NULL OR o.status = :status) " +
		             "AND (:minAmount IS NULL OR o.totalAmount >= :minAmount) " +
		       "AND (:maxAmount IS NULL OR o.totalAmount  <= :maxAmount) " +
		       "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
		       "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
		       "AND (:search IS NULL OR " +
		       "    CAST(o.id AS string) LIKE %:search% OR " +
		       "    u.email LIKE %:search% OR " +
		       "    u.name LIKE %:search%)")
		Page<Order> findOrdersByFilters(
		        @Param("userId") Long userId,
		        @Param("status") OrderStatus status,
		        @Param("minAmount") Double minAmount,
		        @Param("maxAmount") Double maxAmount,
		        @Param("fromDate") Instant fromDate,
		        @Param("toDate") Instant toDate,
		        @Param("search") String search,
		        Pageable pageable);
	   
	   
	   @Query("SELECT DISTINCT o FROM Order o " +
	           "JOIN o.items i " +
	           "WHERE i.productName LIKE %:productName%")
	    List<Order> findByProductName(@Param("productName") String productName);


		 @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId AND o.paymentStatus = 'SUCCESS'")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);
      @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.createdAt >= :since")
    List<Order> findUserOrdersSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o WHERE o.user.id = :userId AND o.id = :orderId")
    boolean isOrderBelongsToUser(@Param("userId") Long userId, @Param("orderId") Long orderId);
    
    @Query("SELECT o.paymentMethod FROM Order o WHERE o.id = :orderId")
    String getPaymentMethodByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT DISTINCT oi.productId FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<Long> findProductIdsByOrderId(@Param("orderId") Long orderId);
    

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
	
}
