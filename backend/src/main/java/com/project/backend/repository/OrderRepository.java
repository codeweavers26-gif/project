package com.project.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.entity.Order;
import com.project.backend.entity.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findAll(Pageable pageable);

	Page<Order> findByUser(User user, Pageable pageable);
}
