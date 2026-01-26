package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.backend.entity.Cart;
import com.project.backend.entity.Product;
import com.project.backend.entity.User;

public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUser(User user);

    Optional<Cart> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Cart c")
    Long countDistinctUsersWithCart();

    @Query("SELECT SUM(c.quantity * c.product.price) FROM Cart c")
    Double getTotalCartValue();

}
