package com.project.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByCartId(Long cartId);
    
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci")
    Long countTotalItems();
    
    Long countByCartId(Long cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);

    @Query("""
        SELECT COALESCE(SUM(ci.variant.sellingPrice * ci.quantity), 0) 
        FROM CartItem ci 
        WHERE ci.cart.updatedAt < :threshold
    """)
    Double sumAbandonedCartValue(@Param("threshold") Instant threshold);
    
    @Query("""
            SELECT ci FROM CartItem ci
            WHERE (:userId IS NULL OR ci.cart.user.id = :userId)
            AND (:productId IS NULL OR ci.variant.product.id = :productId)
            AND (:userEmail IS NULL OR LOWER(ci.cart.user.email) LIKE LOWER(CONCAT('%', :userEmail, '%')))
            AND (:userName IS NULL OR LOWER(ci.cart.user.name) LIKE LOWER(CONCAT('%', :userName, '%')))
            AND (:productName IS NULL OR LOWER(ci.variant.product.name) LIKE LOWER(CONCAT('%', :productName, '%')))
            AND (:minQuantity IS NULL OR ci.quantity >= :minQuantity)
            AND (:maxQuantity IS NULL OR ci.quantity <= :maxQuantity)
            AND (:fromDate IS NULL OR ci.addedAt >= :fromDate)
            AND (:toDate IS NULL OR ci.addedAt <= :toDate)

        """)
        Page<CartItem> findCartItemsWithFilters(
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
    @Query("""
        SELECT ci.variant.product.id, ci.variant.product.name, 
               SUM(ci.quantity), SUM(ci.variant.sellingPrice * ci.quantity),
               COUNT(DISTINCT ci.cart.user.id)
        FROM CartItem ci
        GROUP BY ci.variant.product.id, ci.variant.product.name
        ORDER BY SUM(ci.quantity) DESC
    """)
    List<Object[]> findTopProducts(@Param("limit") int limit);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.addedAt BETWEEN :startDate AND :endDate")
    List<CartItem> findByDateRange(@Param("startDate") Instant startDate, 
                                   @Param("endDate") Instant endDate);
    
    @Query("SELECT p.id, p.name, COUNT(ci) as addCount " +
           "FROM CartItem ci JOIN ci.product p " +
           "GROUP BY p.id, p.name " +
           "ORDER BY addCount DESC")
    List<Object[]> findMostPopularProducts(Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci")
    Long getTotalItemsInCarts();
    
    @Query("SELECT COALESCE(AVG(ci.quantity), 0) FROM CartItem ci")
    Double getAverageQuantity();
    
    @Query("SELECT ci, p, v, u FROM CartItem ci " +
            "JOIN ci.product p " +
            "JOIN ci.variant v " +
            "JOIN ci.cart c " +
            "JOIN c.user u " +
            "WHERE (:userId IS NULL OR u.id = :userId) " +
            "AND (:productId IS NULL OR p.id = :productId)")
     Page<Object[]> findCartItemsWithDetails(
             @Param("userId") Long userId,
             @Param("productId") Long productId,
             Pageable pageable);
     
     
     @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
     List<CartItem> findByUserId(@Param("userId") Long userId);
     
}