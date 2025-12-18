package com.project.backend.requestDto;

import com.project.backend.entity.OrderStatus;

import lombok.Data;

@Data
public class UpdateOrderStatusDto {
	private OrderStatus status;
}