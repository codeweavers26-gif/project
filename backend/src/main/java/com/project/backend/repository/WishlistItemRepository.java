package com.project.backend.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.WishlistItems;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItems, Long> {
    
    Optional<WishlistItems> findByWishlistIdAndProductIdAndVariantId(
        Long wishlistId, Long productId, Long variantId);
    
    List<WishlistItems> findByWishlistUserId(Long userId);
    Long countByWishlistId(Long wishlistId);
    Page<WishlistItems> findByWishlistUserId(Long userId, Pageable pageable);
    
    @Query("SELECT wi FROM WishlistItems wi JOIN FETCH wi.product WHERE wi.wishlist.user.id = :userId")
    List<WishlistItems> findByUserIdWithProduct(@Param("userId") Long userId);
    
    boolean existsByWishlistUserIdAndProductId(Long userId, Long productId);
    
    @Modifying
    @Query("DELETE FROM WishlistItems wi WHERE wi.wishlist.user.id = :userId AND wi.product.id = :productId")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    @Modifying
    @Query("DELETE FROM WishlistItems wi WHERE wi.wishlist.id = :wishlistId")
    void deleteAllByWishlistId(@Param("wishlistId") Long wishlistId);
    
    @Query("SELECT wi.product.id FROM WishlistItems wi WHERE wi.wishlist.user.id = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

	boolean existsByWishlistIdAndProductIdAndVariantId(Long id, Long id2, Long id3);

	int deleteByWishlistIdAndProductIdAndVariantId(Long id, Long productId, Long variantId);
	
	 @Modifying
	    @Query("DELETE FROM WishlistItems wi WHERE wi.id IN :itemIds")
	    void deleteAllByIds(@Param("itemIds") List<Long> itemIds);
	    
}