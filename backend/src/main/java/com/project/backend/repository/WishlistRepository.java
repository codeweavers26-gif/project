package com.project.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.User;
import com.project.backend.entity.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {


    Page<Wishlist> findByUser(User user, Pageable pageable);

     
 Optional<Wishlist> findByUserId(Long userId);
    
    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.items WHERE w.user.id = :userId")
    Optional<Wishlist> findByUserIdWithItems(@Param("userId") Long userId);
    
    boolean existsByUserId(Long userId);
    
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(wi) FROM Wishlist w JOIN w.items wi WHERE w.user.id = :userId")
    Long countItemsByUserId(@Param("userId") Long userId);
    
    
    
  Optional<Wishlist> findByUser(User user);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.items WHERE w.user = :user")
    Optional<Wishlist> findByUserWithItems(@Param("user") User user);
    
    
    @Query("SELECT w, SIZE(w.items) as itemCount FROM Wishlist w WHERE w.user.id = :userId")
    Optional<Object[]> findWithItemCount(@Param("userId") Long userId);
    
    void deleteByUser(User user);
}
