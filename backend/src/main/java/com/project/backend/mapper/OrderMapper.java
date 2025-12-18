package com.project.backend.mapper;

import java.util.stream.Collectors;

import com.project.backend.ResponseDto.OrderItemResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;

public class OrderMapper {

    public static OrderResponseDto toDto(Order order) {

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(
                        order.getItems().stream()
                                .map(item -> OrderItemResponseDto.builder()
                                        .productId(item.getProduct().getId())
                                        .productName(item.getProduct().getName())
                                        .imageUrl(item.getProduct().getImageUrl())
                                        .price(item.getPrice())
                                        .quantity(item.getQuantity())
                                        .totalPrice(item.getPrice() * item.getQuantity())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}
