package com.project.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Cart;
import com.project.backend.entity.User;

public interface CartRepository extends JpaRepository<Cart, Long> {

	List<Cart> findByUser(User user);

//	Optional<Cart> findByUserAndProduct(User user, Product product);

	 Optional<Cart> findByUserId(Long userId);
	void deleteByUser(User user);

	@Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c")
	Long countDistinctUsersWithCart();

	

	void deleteByUserId(Long userId);

	// Count queries
	Long countByUserId(Long userId);

//	@Query("SELECT SUM(c.quantity) FROM Cart c WHERE c.user.id = :userId")
//	Integer sumQuantityByUserId(@Param("userId") Long userId);
	//
	//	@Query("SELECT SUM(c.product.price * c.quantity) FROM Cart c WHERE c.user.id = :userId")
	//	Double sumCartTotalByUserId(@Param("userId") Long userId);

//	  @Query("SELECT SUM(c.quantity * c.product.variants[0].sellingPrice) FROM Cart c WHERE c.user.id = :userId")
//	    Double calculateCartTotal(@Param("userId") Long userId);
//	// Advanced filtering for admin
//	@Query("SELECT DISTINCT c.user.id FROM Cart c")
//	List<Long> findDistinctUserIds();

//	@Query("SELECT c FROM Cart c " + "JOIN FETCH c.user u " + "JOIN FETCH c.product p "
//			+ "WHERE (:userId IS NULL OR c.user.id = :userId) "
//		//	+ "AND (:productId IS NULL OR c.product.id = :productId) "
//			+ "AND (:userEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :userEmail, '%'))) "
//			+ "AND (:userName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :userName, '%'))) "
//			+ "AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) "
//			+ "AND (:minQuantity IS NULL OR c.quantity >= :minQuantity) "
//			+ "AND (:maxQuantity IS NULL OR c.quantity <= :maxQuantity) "
//			+ "AND (:fromDate IS NULL OR c.createdAt >= :fromDate) "
//			+ "AND (:toDate IS NULL OR c.createdAt <= :toDate)")
//	org.springframework.data.domain.Page<Cart> findCartsWithFilters(@Param("userId") Long userId,
//		//	@Param("productId") Long productId,
//			@Param("userEmail") String userEmail,
//			@Param("userName") String userName, @Param("productName") String productName,
//			@Param("minQuantity") Integer minQuantity, @Param("maxQuantity") Integer maxQuantity,
//			@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate, Pageable pageable);

	// Abandoned carts (not checked out for >24h)
//	@Query("SELECT c FROM Cart c WHERE c.createdAt < :threshold")
//	List<Cart> findCartsOlderThan(@Param("threshold") Instant threshold);

	// Group by user for cart summary
//	@Query("SELECT c.user.id, COUNT(c), SUM(c.quantity), SUM(c.product.price * c.quantity) "
//			+ "FROM Cart c GROUP BY c.user.id")
//	List<Object[]> getCartSummaryByUser();
//
//	@Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c")
//	Long countUsersWithCart();
//
//	@Query("SELECT COUNT(c) FROM Cart c")
//	Long countTotalItems();
//
//	@Query("SELECT COALESCE(SUM(c.product.price * c.quantity), 0) FROM Cart c")
//	Double sumTotalCartValue();
//
//	@Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user.id = :userId")
//	Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
//
//	@Query("SELECT c FROM Cart c WHERE c.updatedAt < :threshold AND c.items IS NOT EMPTY")
//	List<Cart> findCartsUpdatedBefore(@Param("threshold") Instant threshold);
//
//	@Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c WHERE c.items IS NOT EMPTY")
//	Long countUsersWithActiveCart();
//
//	@Query("SELECT COUNT(DISTINCT c.id) FROM Cart c WHERE c.updatedAt < :threshold AND c.items IS NOT EMPTY")
//	Long countAbandonedCarts(@Param("threshold") Instant threshold);
//
//	@Query("SELECT DISTINCT c FROM Cart c WHERE c.items IS NOT EMPTY")
//	List<Cart> findAllActiveCarts();
//	
//	
	    
//	    @Query("SELECT COUNT(c) > 0 FROM Cart c WHERE c.user.id = :userId AND c.product.id = :productId")
//	    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
	    
//	    @Query("SELECT SUM(c.quantity) FROM Cart c WHERE c.user.id = :userId")
//	    Integer getTotalItemCountByUserId(@Param("userId") Long userId);
	
	
	
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
	    
	    // 2. Get cart summaries grouped by user
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
	    
	    // 3. Get single user cart details
	    @Query("SELECT c, ci, p, v FROM Cart c " +
	           "LEFT JOIN c.items ci " +
	           "LEFT JOIN ci.product p " +
	           "LEFT JOIN ci.variant v " +
	           "WHERE c.user.id = :userId")
	    Optional<Cart> findUserCartWithDetails(@Param("userId") Long userId);
	    
	     
	    // 5. Cart statistics for dashboard
	    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c WHERE SIZE(c.items) > 0")
	    Long countActiveCarts();
	    
	    @Query("SELECT AVG(SIZE(c.items)) FROM Cart c WHERE SIZE(c.items) > 0")
	    Double averageCartSize();
	
	    
	    // 6. Get carts by date range
	    @Query("SELECT c FROM Cart c WHERE c.createdAt BETWEEN :startDate AND :endDate")
	    List<Cart> findCartsByDateRange(@Param("startDate") Instant startDate, 
	                                    @Param("endDate") Instant endDate);
	    
	    // 7. Get top users by cart value
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
	    
	    // Get carts with items (total_quantity > 0)
	    @Query("SELECT COUNT(c) FROM Cart c WHERE c.totalQuantity > 0")
	    Long countCartsWithItems();
	    
	    
	    @Query("SELECT COUNT(c) FROM Cart c WHERE c.totalQuantity = 0")
	    Long countEmptyCarts();
	    
	    // Get average items per cart
	    @Query("SELECT AVG(c.totalQuantity) FROM Cart c WHERE c.totalQuantity > 0")
	    Double averageItemsPerCart();
	    
	    // Get total value of all carts
	    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM Cart c")
	    Double getTotalCartValue();
	    
	    // Get total value of abandoned carts
	    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM Cart c WHERE c.updatedAt < :cutoffTime AND c.totalQuantity > 0")
	    Double getAbandonedCartValue(@Param("cutoffTime") Instant cutoffTime);
	    
	    // Find abandoned carts
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
