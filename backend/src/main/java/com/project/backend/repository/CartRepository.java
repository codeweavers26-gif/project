package com.project.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Cart;
import com.project.backend.entity.User;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Long> {

	List<Cart> findByUser(User user);

	 Optional<Cart> findByUserId(Long userId);
	void deleteByUser(User user);

	@Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c")
	Long countDistinctUsersWithCart();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
Optional<Cart> findByUserIdForUpdate(Long userId);
	

	void deleteByUserId(Long userId);

	Long countByUserId(Long userId);	
	
	 @Query("SELECT c, ci, u, p, v FROM Cart c " +
	           "JOIN c.items ci " +
	           "JOIN c.user u " +
	           "JOIN ci.product p " +
	           "JOIN ci.variant v " +
	           "WHERE (:userId IS NULL OR u.id = :userId) " +
	           "AND (:productId IS NULL OR p.id = :productId) " +
	           "AND (:userEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :userEmail, '%'))) " +
	           "AND (:userName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :userName, '%'))) " +
	           "AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
	           "AND (:minQuantity IS NULL OR ci.quantity >= :minQuantity) " +
	           "AND (:maxQuantity IS NULL OR ci.quantity <= :maxQuantity) " +
	           "AND (:fromDate IS NULL OR ci.addedAt >= :fromDate) " +
	           "AND (:toDate IS NULL OR ci.addedAt <= :toDate)")
	    Page<Object[]> findCartItemsWithFilters(
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
	    
	    @Query("SELECT NEW map(" +
	           "c.user.id as userId, " +
	           "c.user.email as userEmail, " +
	           "c.user.name as userName, " +
	           "COUNT(ci) as itemCount, " +
	           "SUM(ci.quantity) as totalQuantity, " +
	           "SUM(ci.price * ci.quantity) as totalValue, " +
	           "MAX(c.updatedAt) as lastUpdated, " +
	           "MIN(c.createdAt) as createdAt) " +
	           "FROM Cart c LEFT JOIN c.items ci " +
	           "GROUP BY c.user.id, c.user.email, c.user.name")
	    List<Object[]> findCartSummaries();
	    
	    @Query("SELECT c, ci, p, v FROM Cart c " +
	           "LEFT JOIN c.items ci " +
	           "LEFT JOIN ci.product p " +
	           "LEFT JOIN ci.variant v " +
	           "WHERE c.user.id = :userId")
	    Optional<Cart> findUserCartWithDetails(@Param("userId") Long userId);
	    
	     
	    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c WHERE SIZE(c.items) > 0")
	    Long countActiveCarts();
	    
	    @Query("SELECT AVG(SIZE(c.items)) FROM Cart c WHERE SIZE(c.items) > 0")
	    Double averageCartSize();
	
	    
	    @Query("SELECT c FROM Cart c WHERE c.createdAt BETWEEN :startDate AND :endDate")
	    List<Cart> findCartsByDateRange(@Param("startDate") Instant startDate, 
	                                    @Param("endDate") Instant endDate);
	    
	    @Query("SELECT c.user.id, c.user.email, c.user.name, SUM(ci.price * ci.quantity) as total " +
	           "FROM Cart c JOIN c.items ci " +
	           "GROUP BY c.user.id, c.user.email, c.user.name " +
	           "ORDER BY total DESC")
	    List<Object[]> findTopUsersByCartValue(Pageable pageable);

		    
	    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c WHERE c.totalQuantity > 0")
	    Long countUsersWithActiveCart();
	    
	    @Query("SELECT COUNT(c) FROM Cart c WHERE c.updatedAt < :cutoffTime AND c.totalQuantity > 0")
	    Long countAbandonedCarts(@Param("cutoffTime") Instant cutoffTime);
	    
	    @Query("SELECT COUNT(c) FROM Cart c")
	    Long countTotalCarts();
	    
	    @Query("SELECT COUNT(c) FROM Cart c WHERE c.totalQuantity > 0")
	    Long countCartsWithItems();
	    
	    
	    @Query("SELECT COUNT(c) FROM Cart c WHERE c.totalQuantity = 0")
	    Long countEmptyCarts();
	    
	    @Query("SELECT AVG(c.totalQuantity) FROM Cart c WHERE c.totalQuantity > 0")
	    Double averageItemsPerCart();
	    
	    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM Cart c")
	    Double getTotalCartValue();
	    
	    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM Cart c WHERE c.updatedAt < :cutoffTime AND c.totalQuantity > 0")
	    Double getAbandonedCartValue(@Param("cutoffTime") Instant cutoffTime);
	    
	    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :cutoffTime AND c.totalQuantity > 0")
	    List<Cart> findAbandonedCarts(@Param("cutoffTime") Instant cutoffTime);
	    
		    @Query("SELECT DISTINCT c FROM Cart c " +
		            "LEFT JOIN FETCH c.items ci " +
		            "LEFT JOIN FETCH ci.product " +
		            "LEFT JOIN FETCH ci.variant " +
		            "WHERE c.updatedAt < :threshold AND c.totalQuantity > 0")
	     List<Cart> findAbandonedCartsWithItems(@Param("threshold") Instant threshold);
	    
	    @Query("SELECT DISTINCT c FROM Cart c " +
	            "LEFT JOIN FETCH c.items ci " +
	            "LEFT JOIN FETCH ci.product " +
	            "LEFT JOIN FETCH ci.variant " +
	            "WHERE c.user.id = :userId")
	     Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
	    @Query(value = "SELECT " +
	            "u.id as userId, " +
	            "u.name as userName, " +
	            "u.email as userEmail, " +
	            "COUNT(DISTINCT ci.id) as totalItems, " +
	            "COALESCE(SUM(ci.quantity), 0) as totalQuantity, " +
	            "COALESCE(SUM(ci.price * ci.quantity), 0) as totalValue, " +
	            "MAX(c.updatedAt) as lastActivity " +
	            "FROM Cart c " +
	            "JOIN c.user u " +
	            "LEFT JOIN c.items ci " +
	            "WHERE c.totalQuantity > 0 " +
	            "GROUP BY u.id, u.name, u.email",
	            countQuery = "SELECT COUNT(DISTINCT u.id) FROM Cart c JOIN c.user u WHERE c.totalQuantity > 0")
	     Page<Object[]> findCartSummaries(Pageable pageable);

}
