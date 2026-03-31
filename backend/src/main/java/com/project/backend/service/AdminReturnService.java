package com.project.backend.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AdminUserReturnResponseDto;
import com.project.backend.ResponseDto.EligibilityCheckDto;
import com.project.backend.ResponseDto.EligibleReturnItemDto;
import com.project.backend.ResponseDto.OrderItemDto;
import com.project.backend.ResponseDto.ProductReturnStatsDto;
import com.project.backend.ResponseDto.RefundDto;
import com.project.backend.ResponseDto.ReturnDashboardStats;
import com.project.backend.ResponseDto.ReturnDetailDto;
import com.project.backend.ResponseDto.ReturnDto;
import com.project.backend.ResponseDto.ReturnItemDto;
import com.project.backend.ResponseDto.ReturnReasonStatsDto;
import com.project.backend.ResponseDto.ReturnResponseDto;
import com.project.backend.ResponseDto.ReturnTimelineDto;
import com.project.backend.ResponseDto.ReturnTrackingDto;
import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.PaymentTransaction;
import com.project.backend.entity.Refund;
import com.project.backend.entity.RefundStatus;
import com.project.backend.entity.Return;
import com.project.backend.entity.ReturnItem;
import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;
import com.project.backend.entity.ReturnTimeline;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.PaymentTransactionRepository;
import com.project.backend.repository.RefundRepository;
import com.project.backend.repository.ReturnRepository;
import com.project.backend.repository.ReturnTimelineRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.ApproveReturnRequest;
import com.project.backend.requestDto.CreateReturnRequest;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.RejectReturnRequest;
import com.project.backend.requestDto.ReturnEligibilityRequest;
import com.project.backend.requestDto.ReturnItemRequestDto;
import com.project.backend.requestDto.ReturnRequestDto;
import com.project.backend.requestDto.UpdateReturnStatusRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReturnService {
	private final ReturnRepository returnRepository;
	private final UserRepository userRepository;
	private final CartService cartService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ReturnTimelineRepository timelineRepository;
	private final RefundRepository refundRepository;
	private final ShiprocketService shiprocketService;
	private final RazorpayService razorpayService;

	private final PaymentTransactionRepository paymentTransactionRepository;

public PageResponseDto<ReturnDto> getUserReturns(User user, ReturnStatus status, int page, int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Page<Return> returns;

    if (status != null) {
        String statusString = status.name(); 
        returns = returnRepository.findByUserIdAndStatus(user.getId(), statusString, pageable);
    } else {
        returns = returnRepository.findByUserId(user.getId(), pageable);
    }

    return PageResponseDto.<ReturnDto>builder()
            .content(returns.getContent().stream().map(this::convertToDto).toList())
            .page(returns.getNumber())
            .size(returns.getSize())
            .totalElements(returns.getTotalElements())
            .totalPages(returns.getTotalPages())
            .last(returns.isLast())
            .build();
}
	public ReturnDetailDto getReturnDetails(User user, Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (!returnRecord.getUser().getId().equals(user.getId())) {
				throw new UnauthorizedException("You don't have access to this return");
			}

			return convertToReturnDetailDto(returnRecord);
		} catch (NotFoundException | UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch return details", e);
		}
	}
private ReturnDetailDto convertToReturnDetailDto(Return returnRecord) {

    ReturnDetailDto dto = new ReturnDetailDto();

    dto.setReturnInfo(convertToDto(returnRecord));

    List<OrderItemDto> items = returnRecord.getItems().stream()
            .map(ri -> {

                OrderItem oi = orderItemRepository.findById(ri.getOrderItemId())
                        .orElseThrow();

                BigDecimal price = BigDecimal.valueOf(oi.getPrice());
                BigDecimal total = price.multiply(BigDecimal.valueOf(ri.getQuantity()));

                return OrderItemDto.builder()
                        .productId(oi.getProductId())
                        .variantId(oi.getVariantId())
                        .productName(oi.getProductName())
                        .size(oi.getSize())
                        .color(oi.getColor())
                        .price(price.doubleValue())
                        .quantity(ri.getQuantity())
                        .total(total.doubleValue())
                        .build();
            })
            .toList();

    dto.setItems(items);

    if (returnRecord.getRefund() != null) {
        dto.setRefund(RefundDto.builder()
                .id(returnRecord.getRefund().getId())
                .amount(returnRecord.getRefund().getAmount())
                .status(RefundStatus.valueOf(returnRecord.getStatus()))
                .transactionId(returnRecord.getRefund().getTransactionId())
                .build());
    }

    dto.setTimeline(
            timelineRepository.findByReturnRecordIdOrderByCreatedAtDesc(returnRecord.getId())
                    .stream()
                    .map(this::convertToTimelineDto)
                    .toList()
    );

    return dto;
}
	private ReturnTimelineDto convertToTimelineDto(ReturnTimeline timeline) {
		ReturnTimelineDto dto = new ReturnTimelineDto();
		dto.setId(timeline.getId());
		dto.setReturnId(timeline.getReturnRecord().getId());
		dto.setStatus(timeline.getStatus().toString());
		dto.setTitle(timeline.getTitle());
		dto.setDescription(timeline.getDescription());
		dto.setCreatedAt(timeline.getCreatedAt());
		dto.setCreatedBy(timeline.getCreatedBy());
		dto.setCreatedByRole(timeline.getCreatedByRole());
		return dto;
	}

	public ReturnDto getReturn(Long userId, Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (!returnRecord.getUser().getId().equals(userId)) {
				throw new UnauthorizedException("You don't have access to this return");
			}

			return convertToDto(returnRecord);

		} catch (NotFoundException | UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch return details", e);
		}
	}

	public List<EligibleReturnItemDto> getEligibleItems(Long userId, Long orderId) {
		try {
			Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));

			if (!order.getUser().getId().equals(userId)) {
				throw new UnauthorizedException("You don't have access to this order");
			}
			Instant instant = order.getCreatedAt();
			LocalDateTime purchaseDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
			LocalDateTime returnDeadline = purchaseDate.plusDays(30);
			boolean isWithinWindow = LocalDateTime.now().isBefore(returnDeadline);

			return order.getItems().stream().filter(item -> !returnRepository.existsByOrderItemId(item.getId()))
					.map(item -> {
						EligibleReturnItemDto dto = new EligibleReturnItemDto();
						dto.setOrderItemId(item.getId());
						dto.setProductName(item.getProductName());
						dto.setProductSku(null);
						dto.setPrice(BigDecimal.valueOf(item.getPrice()));
						dto.setQuantity(item.getQuantity());
						dto.setPurchaseDate(purchaseDate);
						dto.setIsEligible(isWithinWindow);
						dto.setReturnDeadline(returnDeadline);
						return dto;
					}).collect(Collectors.toList());

		} catch (NotFoundException | UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch eligible items", e);
		}
	}@Transactional
public ReturnDto cancelReturnRequest(User user, Long returnId) {

    Return returnRecord = returnRepository.findById(returnId)
            .orElseThrow(() -> new NotFoundException("Return not found"));

    if (!returnRecord.getUser().getId().equals(user.getId())) {
        throw new UnauthorizedException("Access denied");
    }

    if (!"PENDING_APPROVAL".equals(returnRecord.getStatus())) {
        throw new IllegalStateException("Cannot cancel now. Current status: " + returnRecord.getStatus());
    }

    returnRecord.setStatus("CANCELLED");
    returnRecord.setUpdatedAt(LocalDateTime.now());

    returnRecord = returnRepository.save(returnRecord);

    return convertToDto(returnRecord);
}

private ReturnDto convertToDto(Return returnRecord) {

    ReturnDto dto = new ReturnDto();

    dto.setId(returnRecord.getId());
    dto.setReturnNumber(returnRecord.getReturnNumber());
    dto.setStatus(returnRecord.getStatus());
    dto.setReason(returnRecord.getReason());
    dto.setReasonDescription(returnRecord.getReasonDescription());
    dto.setRefundAmount(returnRecord.getRefundAmount());
    dto.setRestockingFee(returnRecord.getRestockingFee());
    dto.setTotalRefundAmount(returnRecord.getTotalRefundAmount());
    dto.setCreatedAt(returnRecord.getCreatedAt());
    dto.setUserId(returnRecord.getUser().getId());
    dto.setOrderId(returnRecord.getOrder().getId());

    List<ReturnItemDto> items = returnRecord.getItems().stream()
            .map(ri -> ReturnItemDto.builder()
                    .orderItemId(ri.getOrderItemId())
                    .quantity(ri.getQuantity())
                    .build())
            .toList();

    dto.setItems(items);

    log.info("Converting Return ID: {} to DTO", returnRecord.getId());

    return dto;
}
public EligibilityCheckDto checkReturnEligibility(Long userId, ReturnEligibilityRequest request) {
		try {
			EligibilityCheckDto result = new EligibilityCheckDto();

			OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
					.orElseThrow(() -> new NotFoundException("Order item not found"));

			if (!orderItem.getOrder().getUser().getId().equals(userId)) {
				throw new UnauthorizedException("This item doesn't belong to you");
			}

			boolean returnExists = returnRepository.existsByOrderItemId(orderItem.getId());

			Instant orderInstant = orderItem.getOrder().getCreatedAt();
			LocalDateTime orderDate = LocalDateTime.ofInstant(orderInstant, ZoneId.systemDefault());
			LocalDateTime returnDeadline = orderDate.plusDays(30);
			boolean isWithinWindow = LocalDateTime.now().isBefore(returnDeadline);

			result.setIsEligible(!returnExists && isWithinWindow);
			result.setOrderItemId(request.getOrderItemId());
			result.setProductName(orderItem.getProductName());
			result.setReturnDeadline(returnDeadline);

			if (returnExists) {
				result.setMessage("Return already requested for this item");
			} else if (!isWithinWindow) {
				result.setMessage("Return window has expired");
			} else {
				result.setMessage("Item is eligible for return");
			}

			return result;
		} catch (NotFoundException | UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to check eligibility", e);
		}
	}

	// @Transactional
	// public ReturnDto createReturnRequest(Long userId, CreateReturnRequest request) {
	// 	try {
	// 		User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

	// 		OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
	// 				.orElseThrow(() -> new NotFoundException("Order item not found"));

	// 		if (!orderItem.getOrder().getUser().getId().equals(userId)) {
	// 			throw new UnauthorizedException("This item doesn't belong to you");
	// 		}

	// 		if (returnRepository.existsByOrderItemId(orderItem.getId())) {
	// 			throw new IllegalStateException("Return already requested for this item");
	// 		}

	// 		Instant orderInstant = orderItem.getOrder().getCreatedAt();
	// 		LocalDateTime orderDate = LocalDateTime.ofInstant(orderInstant, ZoneId.systemDefault());
	// 		LocalDateTime returnDeadline = orderDate.plusDays(30);
	// 		if (LocalDateTime.now().isAfter(returnDeadline)) {
	// 			throw new IllegalStateException("Return window has expired");
	// 		}

	// 		if (request.getQuantity() > orderItem.getQuantity()) {
	// 		    throw new BadRequestException(String.format(
	// 		        "Cannot return %d items. You only purchased %d of this item",
	// 		        request.getQuantity(), orderItem.getQuantity()));
	// 		}

	// 		Double itemPrice = orderItem.getPrice();
	// 		if (itemPrice == null || itemPrice <= 0) {
	// 		    throw new IllegalStateException("Invalid item price");
	// 		}

	// 		BigDecimal price = BigDecimal.valueOf(itemPrice);
	// 		BigDecimal refundAmount = price.multiply(BigDecimal.valueOf(request.getQuantity()));

	// 		double totalOrderValue = orderItem.getOrder().getTotalAmount() != null ? 
	// 		                         orderItem.getOrder().getTotalAmount() : 0.0;
	// 		double itemValue = itemPrice * request.getQuantity();

	// 		BigDecimal discountAmount = BigDecimal.ZERO;
	// 		if (orderItem.getOrder().getDiscountAmount() != null && 
	// 		    orderItem.getOrder().getDiscountAmount() > 0 && 
	// 		    totalOrderValue > 0) {
			    
	// 		    double discountRatio = itemValue / totalOrderValue;
	// 		    discountAmount = BigDecimal.valueOf(orderItem.getOrder().getDiscountAmount() * discountRatio);
	// 		}

	// 		BigDecimal taxAmount = BigDecimal.ZERO;
	// 		if (orderItem.getOrder().getTaxAmount() != null && 
	// 		    orderItem.getOrder().getTaxAmount() > 0 && 
	// 		    totalOrderValue > 0) {

	// 		    double taxRatio = itemValue / totalOrderValue;
	// 		    taxAmount = BigDecimal.valueOf(orderItem.getOrder().getTaxAmount() * taxRatio);
	// 		}
	// 		Return returnRecord = new Return();
	// 		returnRecord.setUser(user);
	// 		returnRecord.setOrder(orderItem.getOrder());
	// 		returnRecord.setOrderItem(orderItem);
	// 		returnRecord.setReason(request.getReason());
	// 		returnRecord.setReasonDescription(request.getReasonDescription());
	// 		returnRecord.setQuantity(request.getQuantity());
	// 		// returnRecord.setImageUrls(request.getImageUrls());
	// 		returnRecord.setStatus(ReturnStatus.PENDING_APPROVAL);

			
			
	// 	        returnRecord.setRefundAmount(refundAmount);
	// 	        returnRecord.setRestockingFee(BigDecimal.ZERO);
	// 	        returnRecord.setTotalRefundAmount(refundAmount); 
	// 	        returnRecord.setShippingCost(BigDecimal.ZERO); 

	// 	        returnRecord.setRefundAmount(refundAmount);
	// 	        returnRecord.setRestockingFee(BigDecimal.ZERO); 
	// 	        returnRecord.setTotalRefundAmount(refundAmount.subtract(discountAmount));
	// 	        returnRecord.setShippingCost(calculateShippingCost(orderItem.getOrder(), request.getQuantity()));
	// 	        log.debug("=== SAVING RETURN WITH VALUES ===");
	// 	        log.debug("returnNumber: {}", returnRecord.getReturnNumber());
	// 	        log.debug("status: {}", returnRecord.getStatus());
	// 	        log.debug("quantity: {}", returnRecord.getQuantity());
	// 	        log.debug("reason: {}", returnRecord.getReason());
	// 	        log.debug("refundAmount: {}", returnRecord.getRefundAmount());
	// 	        log.debug("restockingFee: {}", returnRecord.getRestockingFee());
	// 	        log.debug("totalRefundAmount: {}", returnRecord.getTotalRefundAmount());
	// 	        log.debug("shippingCost: {}", returnRecord.getShippingCost());
	// 	        log.debug("createdAt: {}", returnRecord.getCreatedAt());
	// 	        log.debug("updatedAt: {}", returnRecord.getUpdatedAt());
	// 	        log.debug("================================");
	// 		returnRecord = returnRepository.save(returnRecord);

	// 		createTimelineEntry(returnRecord, "Return requested", "USER", user.getEmail());

	// 		return convertToDto(returnRecord);
	// 	} catch (NotFoundException | UnauthorizedException | IllegalStateException e) {
	// 		throw e;
	// 	} catch (Exception e) {
	// 		throw new RuntimeException("Failed to create return request", e);
	// 	}
	// }

	
	private BigDecimal calculateShippingCost(Order order, int returnQuantity) {

	    
	    if (returnQuantity <= 3) {
	        return BigDecimal.valueOf(50); 
	    } else {
	        return BigDecimal.valueOf(100);
	    }
	}
	
	@Transactional
public void retryReturn(Long returnId) {

    Return returnRecord = returnRepository.findById(returnId)
            .orElseThrow();

    if (returnRecord.getStatus() != "FAILED") {
        throw new RuntimeException("Retry not allowed");
    }

    approveReturn(returnId);
}

	// public ReturnTrackingDto trackReturn(User user, Long returnId) {
	// 	try {
	// 		Return returnRecord = returnRepository.findById(returnId)
	// 				.orElseThrow(() -> new NotFoundException("Return not found"));

	// 		if (!returnRecord.getUser().getId().equals(user.getId())) {
	// 			throw new UnauthorizedException("You don't have access to this return");
	// 		}

	// 		ReturnTrackingDto trackingDto = new ReturnTrackingDto();
	// 		trackingDto.setReturnNumber(returnRecord.getReturnNumber());
	// 		trackingDto.setCurrentStatus(returnRecord.getStatus());

	// 		List<ReturnTimeline> timeline = timelineRepository.findByReturnRecordIdOrderByCreatedAtDesc(returnId);

	// 		List<ReturnTrackingDto.TrackingStep> steps = timeline.stream().map(t -> {
	// 			ReturnTrackingDto.TrackingStep step = new ReturnTrackingDto.TrackingStep();
	// 			step.setTitle(t.getTitle());
	// 			step.setDescription(t.getDescription());
	// 			step.setDate(t.getCreatedAt());
	// 			step.setCompleted(true);
	// 			return step;
	// 		}).collect(Collectors.toList());

	// 		trackingDto.setSteps(steps);

	// 		if (returnRecord.getStatus() == "APPROVED") {
	// 			trackingDto.setEstimatedCompletionDate(LocalDateTime.now().plusDays(5));
	// 		} else if (returnRecord.getStatus() == ReturnStatus.QC_PASSED) {
	// 			trackingDto.setEstimatedCompletionDate(LocalDateTime.now().plusDays(2));
	// 		}

	// 		return trackingDto;
	// 	} catch (NotFoundException | UnauthorizedException e) {
	// 		throw e;
	// 	} catch (Exception e) {
	// 		throw new RuntimeException("Failed to track return", e);
	// 	}
	// }

	// public List<ReturnTimelineDto> getReturnTimeline(User user, Long returnId) {
	// 	try {
	// 		Return returnRecord = returnRepository.findById(returnId)
	// 				.orElseThrow(() -> new NotFoundException("Return not found"));

	// 		if (!returnRecord.getUser().getId().equals(user.getId())) {
	// 			throw new UnauthorizedException("You don't have access to this return");
	// 		}

	// 		List<ReturnTimeline> timeline = timelineRepository.findByReturnRecordIdOrderByCreatedAtDesc(returnId);

	// 		return timeline.stream().map(this::convertToTimelineDto).collect(Collectors.toList());
	// 	} catch (NotFoundException | UnauthorizedException e) {
	// 		throw e;
	// 	} catch (Exception e) {
	// 		throw new RuntimeException("Failed to fetch return timeline", e);
	// 	}
	// }

	// private void createTimelineEntry(Return returnRecord, String title, String role, String createdBy) {
	// 	ReturnTimeline timeline = ReturnTimeline.builder().returnRecord(returnRecord).status(returnRecord.getStatus())
	// 			.title(title).createdBy(createdBy).createdByRole(role).isCustomerVisible(true).isAdminVisible(true)
	// 			.build();
	// 	timelineRepository.save(timeline);
	// }
public PageResponseDto<ReturnDto> getAllReturns(ReturnStatus status, String search, Long productId,
        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
    try { 
		  String statusString = status != null ? status.name() : null;
        
        Page<Return> returns = returnRepository.findByFilters(statusString, search, productId, fromDate, toDate, pageable);
        
        return PageResponseDto.<ReturnDto>builder()
                .content(returns.getContent().stream().map(this::convertToDto).collect(Collectors.toList()))
                .page(returns.getNumber())
                .size(returns.getSize())
                .totalElements(returns.getTotalElements())
                .totalPages(returns.getTotalPages())
                .last(returns.isLast())
                .build();
    } catch (Exception e) {
        throw new RuntimeException("Failed to fetch returns", e);
    }
}
	

	public ReturnDetailDto getReturnById(Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));
			return convertToReturnDetailDto(returnRecord);
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch return", e);
		}
	}

	public ReturnDto getReturnByOrderItem(Long orderItemId) {
		try {
			Return returnRecord = returnRepository.findByOrderItemId(orderItemId)
					.orElseThrow(() -> new NotFoundException("Return not found for this order item"));
			return convertToDto(returnRecord);
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch return by order item", e);
		}
	}

public List<ReturnDto> getPendingReturns() {
    try {
        List<Return> returns = returnRepository.findByStatus("PENDING_APPROVAL");
        return returns.stream().map(this::convertToDto).collect(Collectors.toList());
    } catch (Exception e) {
        throw new RuntimeException("Failed to fetch pending returns", e);
    }
}

	@Transactional
	public ReturnDto updateReturnStatus(Long returnId, UpdateReturnStatusRequest request,User user) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			String oldStatus = returnRecord.getStatus();
			returnRecord.setStatus(request.getStatus());

			returnRecord = returnRepository.save(returnRecord);

			return convertToDto(returnRecord);
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to update return status", e);
		}
	}

@Transactional
public ReturnDto rejectReturn(Long returnId, RejectReturnRequest request, User user) {
    try {
        Return returnRecord = returnRepository.findById(returnId)
                .orElseThrow(() -> new NotFoundException("Return not found"));

        if (!"PENDING_APPROVAL".equals(returnRecord.getStatus())) {
            throw new IllegalStateException(
                    "Return cannot be rejected in current state: " + returnRecord.getStatus());
        }

        returnRecord.setStatus("REJECTED");
        returnRecord.setRejectedAt(LocalDateTime.now());
        returnRecord.setRejectionReason(request.getRejectionReason());
        returnRecord.setUpdatedAt(LocalDateTime.now()); 

        returnRecord = returnRepository.save(returnRecord);
        
        return convertToDto(returnRecord);
        
    } catch (NotFoundException | IllegalStateException e) {
        throw e;
    } catch (Exception e) {
        log.error("Failed to reject return {}: {}", returnId, e.getMessage(), e);
        throw new RuntimeException("Failed to reject return", e);
    }
}
	
	public ReturnDashboardStats getReturnDashboardStats() {
    try {
        ReturnDashboardStats stats = new ReturnDashboardStats();

        stats.setTotalReturns(returnRepository.count());
       
        stats.setPendingApproval(returnRepository.countByStatus("PENDING_APPROVAL"));
        stats.setApproved(returnRepository.countByStatus("APPROVED"));
        stats.setRejected(returnRepository.countByStatus("REJECTED"));
        stats.setCompleted(returnRepository.countByStatus("REFUND_COMPLETED"));

        Map<String, Long> statusCounts = returnRepository.countByStatusGrouped();
        
        Map<ReturnStatus, Long> convertedStatusCounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            try {
                convertedStatusCounts.put(ReturnStatus.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown status string: {}", entry.getKey());
            }
        }
        stats.setReturnsByStatus(convertedStatusCounts);

        return stats;
    } catch (Exception e) {
        throw new RuntimeException("Failed to fetch dashboard stats", e);
    }
}

	public PageResponseDto<ProductReturnStatsDto> getTopReturnedProducts(int page, int size) {
	    try {
	        Pageable pageable = PageRequest.of(page, size, Sort.by("returnCount").descending());
	        Page<ProductReturnStatsDto> productPage = returnRepository.getTopReturnedProducts(pageable);

	        return PageResponseDto.<ProductReturnStatsDto>builder()
	                .content(productPage.getContent())
	                .page(productPage.getNumber())
	                .size(productPage.getSize())
	                .totalElements(productPage.getTotalElements())
	                .totalPages(productPage.getTotalPages())
	                .last(productPage.isLast())
	                .build();
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to fetch top returned products", e);
	    }
	}

	public PageResponseDto<ReturnReasonStatsDto> getReturnReasonAnalytics(
	        LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
	    try {
	        List<Object[]> results = returnRepository.getReturnReasonAnalyticsRaw(fromDate, toDate);
	        
	        Long totalCount = results.stream()
	                .map(row -> (Long) row[1])
	                .reduce(0L, Long::sum);
	        
	        List<ReturnReasonStatsDto> statsList = results.stream()
	                .map(row -> {
	                    ReturnReason reason = (ReturnReason) row[0];
	                    Long count = (Long) row[1];
	                    BigDecimal totalRefund = (BigDecimal) row[2];
	                    
	                    Double percentage = totalCount > 0 ? 
	                        (count.doubleValue() / totalCount.doubleValue()) * 100 : 0.0;
	                    
	                    ReturnReasonStatsDto dto = new ReturnReasonStatsDto();
	                    dto.setReason(reason);
	                  //  dto.setDescription(reason != null ? reason.getDescription() : "");
	                    dto.setCount(count);
	                    dto.setPercentage(percentage);
	                    dto.setTotalRefundAmount(totalRefund);
	                    
	                    return dto;
	                })
	                .collect(Collectors.toList());
	        
	        int start = page * size;
	        int end = Math.min(start + size, statsList.size());
	        List<ReturnReasonStatsDto> paginatedContent = statsList.subList(start, end);
	        
	        return PageResponseDto.<ReturnReasonStatsDto>builder()
	                .content(paginatedContent)
	                .page(page)
	                .size(size)
	                .totalElements(statsList.size())
	                .totalPages((int) Math.ceil((double) statsList.size() / size))
	                .last((start + size) >= statsList.size())
	                .build();
	                
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to fetch return reason analytics", e);
	    }
	}
	
	
	public PageResponseDto<AdminUserReturnResponseDto> getReturnsByUser(Long userId, int page, int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Page<Return> returns = returnRepository.findByUserId(userId, pageable);

    return PageResponseDto.<AdminUserReturnResponseDto>builder()
            .content(returns.getContent().stream()
                    .map(this::convertToAdminUserReturnResponseDto)
                    .toList())
            .page(returns.getNumber())
            .size(returns.getSize())
            .totalElements(returns.getTotalElements())
            .totalPages(returns.getTotalPages())
            .last(returns.isLast())
            .build();
}

	private AdminUserReturnResponseDto convertToAdminUserReturnResponseDto(Return returnRecord) {

    AdminUserReturnResponseDto dto = new AdminUserReturnResponseDto();

    dto.setReturnId(returnRecord.getId());
    dto.setReturnNumber(returnRecord.getReturnNumber());
    dto.setStatus(returnRecord.getStatus());
    dto.setReason(returnRecord.getReason());

    int totalQty = returnRecord.getItems().stream()
            .mapToInt(ReturnItem::getQuantity)
            .sum();

    dto.setQuantity(totalQty);

    dto.setRefundAmount(returnRecord.getTotalRefundAmount());
    dto.setCreatedAt(returnRecord.getCreatedAt());
    dto.setOrderId(returnRecord.getOrder().getId());
    dto.setUserEmail(returnRecord.getUser().getEmail());

    List<ReturnItemDto> items = returnRecord.getItems().stream()
            .map(ri -> ReturnItemDto.builder()
                    .orderItemId(ri.getOrderItemId())
                    .quantity(ri.getQuantity())
                    .build())
            .toList();

    dto.setItems(items); 

    return dto;
}
@Transactional
public Return requestReturn(User user, Long orderId, ReturnRequestDto request) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));

    if (!order.getUser().getId().equals(user.getId())) {
        throw new BadRequestException("Unauthorized - This order does not belong to you");
    }

    if (!"DELIVERED".equals(order.getStatus().name())) {
        throw new BadRequestException("Return allowed only for delivered orders");
    }
    if (returnRepository.findByOrderId(orderId).isPresent()) {
        throw new BadRequestException("Return already requested for this order");
    }

    int totalQuantity = 0;
    BigDecimal totalRefundAmount = BigDecimal.ZERO;
    List<ReturnItem> returnItems = new ArrayList<>();
    
    for (ReturnItemRequestDto itemReq : request.getItems()) {
     
        OrderItem orderItem = orderItemRepository
                .findById(itemReq.getOrderItemId())
                .orElseThrow(() -> new NotFoundException("Order item not found with ID: " + itemReq.getOrderItemId()));

        if (!orderItem.getOrder().getId().equals(orderId)) {
            throw new BadRequestException("Item " + itemReq.getOrderItemId() + 
                                         " does not belong to order " + orderId);
        }

        if (itemReq.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0 for item: " + orderItem.getProductName());
        }
        
        if (itemReq.getQuantity() > orderItem.getQuantity()) {
            throw new BadRequestException("Cannot return " + itemReq.getQuantity() + 
                                         " items for " + orderItem.getProductName() + 
                                         ". Only " + orderItem.getQuantity() + " purchased.");
        }

        if (returnRepository.existsByOrderItemId(orderItem.getId())) {
            throw new BadRequestException("Item " + orderItem.getProductName() + 
                                         " is already part of a return request");
        }

        totalQuantity += itemReq.getQuantity();
        BigDecimal itemRefund = BigDecimal.valueOf(orderItem.getPrice())
                .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
        totalRefundAmount = totalRefundAmount.add(itemRefund);
        
        returnItems.add(ReturnItem.builder()
                .orderItemId(orderItem.getId())
                .quantity(itemReq.getQuantity())
                .build());
    }

    Return ret = Return.builder()
            .user(user)
            .order(order)
            .reason(request.getReason())
            .reasonDescription(request.getComment())
            .quantity(totalQuantity)  
            .status("PENDING_APPROVAL")
            .refundAmount(totalRefundAmount)  
            .totalRefundAmount(totalRefundAmount) 
            .restockingFee(BigDecimal.ZERO) 
            .shippingCost(BigDecimal.ZERO)  
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    returnItems.forEach(item -> item.setReturnEntity(ret));
    ret.setItems(returnItems);

    Return savedReturn = returnRepository.save(ret);
    
    log.info("Return request created for order {} with {} items", orderId, returnItems.size());
    
    return savedReturn;
}

@Transactional
public ReturnDto  approveReturn(Long returnId) {

    Return ret = returnRepository.findById(returnId)
            .orElseThrow(() -> new NotFoundException("Return not found"));
    Order order = orderRepository.findById(ret.getOrder().getId()).orElseThrow();

			if (!ret.getStatus().equals("PENDING_APPROVAL")){
				throw new BadRequestException("Invalid state");
			}

			if(ret.getShipmentId() != null){
				return  convertToDto(ret);
			}

    ShipmentResponse shipment = shiprocketService.createReturnShipment(order);

    ShipmentResponse assigned =
            shiprocketService.assignCourier(shipment.getShipmentId());

    ret.setShipmentId(shipment.getShipmentId());
    ret.setTrackingId(assigned.getTrackingId());

	shiprocketService.generatePickup(shipment.getShipmentId());
    ret.setStatus("PICKUP_SCHEDULED");
    ret.setUpdatedAt(LocalDateTime.now());

   Return updatedReturn =  returnRepository.save(ret);

	   return convertToDto(updatedReturn);
}

public ReturnResponseDto getReturn(Long orderId) {

    Return ret = returnRepository.findByOrderId(orderId)
            .orElseThrow(() -> new NotFoundException("No return found"));

List<ReturnItemDto> items = ret.getItems() != null 
    ? ret.getItems().stream()
        .map(ri -> ReturnItemDto.builder()
                .orderItemId(ri.getOrderItemId())
                .quantity(ri.getQuantity())
                .build())
        .collect(Collectors.toList())
    : new ArrayList<>();
    return ReturnResponseDto.builder()
            .orderId(orderId)
            .status(ret.getStatus())
            .reason(ret.getReason())
            .trackingId(ret.getTrackingId())
            .refundAmount(ret.getRefundAmount())
            .items(items)
            .build();
}
@Transactional
public void processRefund(Long returnId) {

    if (refundRepository.existsByReturnRequestId(returnId)) {
        log.info("Refund already processed for returnId={}", returnId);
        return;
    }

    Return ret = returnRepository.findById(returnId)
            .orElseThrow(() -> new NotFoundException("Return not found"));

    PaymentTransaction txn = paymentTransactionRepository
            .findByOrderId(ret.getOrder().getId())
            .orElseThrow(() -> new RuntimeException("Payment not found"));

    if (txn.getRazorpayPaymentId() == null) {
        throw new RuntimeException("No paymentId found for refund");
    }

    Refund refund = Refund.builder()
            .returnRequest(ret)
            .amount(ret.getTotalRefundAmount())
            .status("INITIATED")
            .createdAt(LocalDateTime.now())
            .build();

    refundRepository.save(refund);

    try {
        String refundId = razorpayService.refundPayment(
                txn.getRazorpayPaymentId(),
                refund.getAmount()
        );

        refund.setTransactionId(refundId);
        refund.setStatus("SUCCESS");

        ret.setStatus("REFUNDED");

    } catch (Exception e) {

        refund.setStatus("FAILED");
        refund.setFailureReason(e.getMessage());

        ret.setStatus("FAILED");
    }

    refund.setUpdatedAt(LocalDateTime.now());

    refundRepository.save(refund);
}
}
