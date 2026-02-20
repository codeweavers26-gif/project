package com.project.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Cart;
import com.project.backend.entity.Product;
import com.project.backend.entity.User;

public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUser(User user);

    Optional<Cart> findByUserAndProduct(User user, Product product);
    List<Cart> findByUserId(Long userId);
    void deleteByUser(User user);
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c")
    Long countDistinctUsersWithCart();

    @Query("SELECT SUM(c.quantity * c.product.price) FROM Cart c")
    Double getTotalCartValue();

    
    void deleteByUserId(Long userId);
    
    // Count queries
    Long countByUserId(Long userId);
    
    @Query("SELECT SUM(c.quantity) FROM Cart c WHERE c.user.id = :userId")
    Integer sumQuantityByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(c.product.price * c.quantity) FROM Cart c WHERE c.user.id = :userId")
    Double sumCartTotalByUserId(@Param("userId") Long userId);
    
    // Advanced filtering for admin
    @Query("SELECT DISTINCT c.user.id FROM Cart c")
    List<Long> findDistinctUserIds();
    
    @Query("SELECT c FROM Cart c " +
           "JOIN FETCH c.user u " +
           "JOIN FETCH c.product p " +
           "WHERE (:userId IS NULL OR c.user.id = :userId) " +
           "AND (:productId IS NULL OR c.product.id = :productId) " +
           "AND (:userEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :userEmail, '%'))) " +
           "AND (:userName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :userName, '%'))) " +
           "AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
           "AND (:minQuantity IS NULL OR c.quantity >= :minQuantity) " +
           "AND (:maxQuantity IS NULL OR c.quantity <= :maxQuantity) " +
           "AND (:fromDate IS NULL OR c.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR c.createdAt <= :toDate)")
    org.springframework.data.domain.Page<Cart> findCartsWithFilters(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("userEmail") String userEmail,
            @Param("userName") String userName,
            @Param("productName") String productName,
            @Param("minQuantity") Integer minQuantity,
            @Param("maxQuantity") Integer maxQuantity,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
    
    // Abandoned carts (not checked out for >24h)
    @Query("SELECT c FROM Cart c WHERE c.createdAt < :threshold")
    List<Cart> findCartsOlderThan(@Param("threshold") Instant threshold);
    
    // Group by user for cart summary
    @Query("SELECT c.user.id, COUNT(c), SUM(c.quantity), SUM(c.product.price * c.quantity) " +
           "FROM Cart c GROUP BY c.user.id")
    List<Object[]> getCartSummaryByUser();
    
    // Dashboard statistics
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c")
    Long countUsersWithCart();
    
    @Query("SELECT COUNT(c) FROM Cart c")
    Long countTotalItems();
    
    @Query("SELECT COALESCE(SUM(c.product.price * c.quantity), 0) FROM Cart c")
    Double sumTotalCartValue();}
