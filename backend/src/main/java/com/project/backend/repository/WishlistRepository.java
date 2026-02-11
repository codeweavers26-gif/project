package com.project.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Product;
import com.project.backend.entity.User;
import com.project.backend.entity.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserAndProduct(User user, Product product);

    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    Page<Wishlist> findByUser(User user, Pageable pageable);

    void deleteByUserAndProduct(User user, Product product);
}
