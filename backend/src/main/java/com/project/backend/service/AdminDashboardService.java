package com.project.backend.service;

import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AdminDashboardResponse;
import com.project.backend.entity.Role;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    public AdminDashboardResponse getDashboardMetrics() {

        Double totalSales = orderRepository.getTotalSales();
        Long totalOrders = orderRepository.countDeliveredOrders();
        Long totalItemsSold = orderRepository.getTotalItemsSold();
        Long totalInventoryLeft = productRepository.getTotalStock();
        Long totalUsers = userRepository.countByRole(Role.CUSTOMER);

        Long usersWithCart = cartRepository.countDistinctUsersWithCart();
        Double totalCartValue = cartRepository.getTotalCartValue();

        return AdminDashboardResponse.builder()
                .totalSales(totalSales != null ? totalSales : 0)
                .totalOrders(totalOrders != null ? totalOrders : 0)
                .totalItemsSold(totalItemsSold != null ? totalItemsSold : 0)
                .totalInventoryLeft(totalInventoryLeft != null ? totalInventoryLeft : 0)
                .totalUsers(totalUsers != null ? totalUsers : 0)
                .usersWithCart(usersWithCart != null ? usersWithCart : 0)
                .totalCartValue(totalCartValue != null ? totalCartValue : 0)
                .build();
    }
}
