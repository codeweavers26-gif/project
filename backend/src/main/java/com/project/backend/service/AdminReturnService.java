package com.project.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AdminReturnResponseDto;
import com.project.backend.ResponseDto.AdminUserReturnResponseDto;
import com.project.backend.ResponseDto.ReturnByReasonDto;
import com.project.backend.ResponseDto.ReturnResponseDto;
import com.project.backend.entity.OrderReturn;
import com.project.backend.entity.ReturnStatus;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.OrderReturnRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.UpdateReturnStatusDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReturnService {

	private final OrderReturnRepository returnRepo;

	private final UserRepository userRepository;

	public PageResponseDto<AdminReturnResponseDto> getAllReturns(int page, int size) {

		Page<OrderReturn> returns = returnRepo.findAll(PageRequest.of(page, size, Sort.by("requestedAt").descending()));

		return PageResponseDto.<AdminReturnResponseDto>builder()
				.content(returns.getContent().stream().map(this::mapToAdminDto).toList()).page(returns.getNumber())
				.size(returns.getSize()).totalElements(returns.getTotalElements()).totalPages(returns.getTotalPages())
				.last(returns.isLast()).build();
	}

	private AdminReturnResponseDto mapToAdminDto(OrderReturn r) {

		return AdminReturnResponseDto.builder().returnId(r.getId()).orderId(r.getOrderItem().getOrder().getId())
				.userId(r.getOrderItem().getOrder().getUser().getId())
				.userEmail(r.getOrderItem().getOrder().getUser().getEmail())
				.productName(r.getOrderItem().getProduct().getName()).quantity(r.getReturnQuantity())
				.reason(r.getReason().name()).status(r.getStatus().name()).refundAmount(r.getRefundAmount())
				.requestedAt(r.getRequestedAt()).build();
	}

	@Transactional
	public void updateReturnStatus(Long returnId, UpdateReturnStatusDto dto) {

		OrderReturn r = returnRepo.findById(returnId).orElseThrow(() -> new NotFoundException("Return not found"));

		r.setStatus(ReturnStatus.valueOf(dto.getStatus()));
		r.setRefundAmount(dto.getRefundAmount());
		r.setAdminComment(dto.getAdminComment());

		returnRepo.save(r);
	}

	public PageResponseDto<AdminUserReturnResponseDto> getReturnsByUser(Long userId, int page, int size) {

		userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

		Page<OrderReturn> returns = returnRepo.findByOrderItem_Order_User_Id(userId,
				PageRequest.of(page, size, Sort.by("requestedAt").descending()));

		return PageResponseDto.<AdminUserReturnResponseDto>builder()
				.content(returns.getContent().stream().map(this::mapToUserReturnDto).toList()).page(returns.getNumber())
				.size(returns.getSize()).totalElements(returns.getTotalElements()).totalPages(returns.getTotalPages())
				.last(returns.isLast()).build();
	}

	private AdminUserReturnResponseDto mapToUserReturnDto(OrderReturn r) {

		return AdminUserReturnResponseDto.builder().returnId(r.getId())
				.userId(r.getOrderItem().getOrder().getUser().getId())
				.userEmail(r.getOrderItem().getOrder().getUser().getEmail())
				.orderId(r.getOrderItem().getOrder().getId()).orderItemId(r.getOrderItem().getId())
				.productId(r.getOrderItem().getProduct().getId()).productName(r.getOrderItem().getProduct().getName())
				.quantity(r.getReturnQuantity()).reason(r.getReason().name()).status(r.getStatus().name())
				.refundAmount(r.getRefundAmount()).requestedAt(r.getRequestedAt()).build();
	}

	private ReturnResponseDto mapToReturnDto(OrderReturn r) {

		return ReturnResponseDto.builder().returnId(r.getId()).orderId(r.getOrderItem().getOrder().getId())
				.orderItemId(r.getOrderItem().getId()).productId(r.getOrderItem().getProduct().getId())
				.productName(r.getOrderItem().getProduct().getName()).quantity(r.getReturnQuantity())
				.status(r.getStatus()).reason(r.getReason().name()) // ✅ FIXED
				.refundAmount(r.getRefundAmount()).requestedAt(r.getRequestedAt()).build();
	}

	private AdminUserReturnResponseDto mapToDto(OrderReturn r) {

		return AdminUserReturnResponseDto.builder().returnId(r.getId())
				.userId(r.getOrderItem().getOrder().getUser().getId())
				.userEmail(r.getOrderItem().getOrder().getUser().getEmail())
				.orderId(r.getOrderItem().getOrder().getId()).orderItemId(r.getOrderItem().getId())
				.productId(r.getOrderItem().getProduct().getId()).productName(r.getOrderItem().getProduct().getName())
				.quantity(r.getReturnQuantity()).reason(r.getReason().name()).status(r.getStatus().name())
				.refundAmount(r.getRefundAmount()).requestedAt(r.getRequestedAt()).build();
	}

	public List<ReturnByReasonDto> getReturnsByReason() {

	    return returnRepo.countByReason().stream()
	            .map(r -> ReturnByReasonDto.builder()
	                    .reason(r[0].toString())
	                    .count((Long) r[1])
	                    .build())
	            .toList();
	}

	public PageResponseDto<ReturnResponseDto> getUserReturns(Long userId, int page, int size) {

		Page<OrderReturn> returns = returnRepo.findByOrderItem_Order_User_Id(userId,
				PageRequest.of(page, size, Sort.by("createdAt").descending()));

		return mapToPageDto(returns);
	}

	public PageResponseDto<ReturnResponseDto> adminSearchReturns(Long userId, ReturnStatus status, int page, int size) {

		Page<OrderReturn> returns = returnRepo.search(userId, status,
				PageRequest.of(page, size, Sort.by("createdAt").descending()));

		return mapToPageDto(returns);
	}

	private PageResponseDto<ReturnResponseDto> mapToPageDto(Page<OrderReturn> page) {

		List<ReturnResponseDto> content = page.getContent().stream().map(this::mapToReturnDto).toList();

		return PageResponseDto.<ReturnResponseDto>builder().content(content).page(page.getNumber()).size(page.getSize())
				.totalElements(page.getTotalElements()).totalPages(page.getTotalPages()).last(page.isLast()).build();
	}

	public PageResponseDto<ReturnResponseDto> getReturnsByStatus(
            ReturnStatus status,
            int page,
            int size
    ) {

        Page<OrderReturn> returns =
        		returnRepo.findByStatus(
                        status,
                        PageRequest.of(page, size, Sort.by("requestedAt").descending())
                );

        return PageResponseDto.<ReturnResponseDto>builder()
                .content(
                        returns.getContent().stream()
                                .map(r -> ReturnResponseDto.builder()
                                        .returnId(r.getId())
                                        .orderId(r.getOrderItem().getOrder().getId())
                                        .orderItemId(r.getOrderItem().getId())
                                        .productId(r.getOrderItem().getProduct().getId())
                                        .productName(r.getOrderItem().getProduct().getName())
                                        .quantity(r.getReturnQuantity())
                                        .status(r.getStatus())
                                        .reason(r.getReason().name())
                                        .refundAmount(r.getRefundAmount())
                                        .requestedAt(r.getRequestedAt())
                                        .build()
                                )
                                .toList()
                )
                .page(returns.getNumber())
                .size(returns.getSize())
                .totalElements(returns.getTotalElements())
                .totalPages(returns.getTotalPages())
                .last(returns.isLast())
                .build();
    }
}
