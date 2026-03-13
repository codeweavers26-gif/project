package com.project.backend.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
import com.project.backend.ResponseDto.ReturnReasonStatsDto;
import com.project.backend.ResponseDto.ReturnTimelineDto;
import com.project.backend.ResponseDto.ReturnTrackingDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.Return;
import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;
import com.project.backend.entity.ReturnTimeline;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.OrderReturnRepository;
import com.project.backend.repository.RefundRepository;
import com.project.backend.repository.ReturnRepository;
import com.project.backend.repository.ReturnTimelineRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.ApproveReturnRequest;
import com.project.backend.requestDto.CreateReturnRequest;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.RejectReturnRequest;
import com.project.backend.requestDto.ReturnEligibilityRequest;
import com.project.backend.requestDto.UpdateReturnStatusRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReturnService {
	private final ReturnRepository returnRepository;
	private final OrderReturnRepository returnRepo;
	private final UserRepository userRepository;
	private final CartService cartService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderReturnRepository orderReturnRepository;
	private final ReturnTimelineRepository timelineRepository;
	private final RefundRepository refundRepository;
//	public PageResponseDto<AdminReturnResponseDto> getAllReturns(int page, int size) {
//
//		Page<OrderReturn> returns = returnRepo.findAll(PageRequest.of(page, size, Sort.by("requestedAt").descending()));
//
//		return PageResponseDto.<AdminReturnResponseDto>builder()
//				.content(returns.getContent().stream().map(this::mapToAdminDto).toList()).page(returns.getNumber())
//				.size(returns.getSize()).totalElements(returns.getTotalElements()).totalPages(returns.getTotalPages())
//				.last(returns.isLast()).build();
//	}
//
//	private AdminReturnResponseDto mapToAdminDto(OrderReturn r) {
//
//		return AdminReturnResponseDto.builder().returnId(r.getId()).orderId(r.getOrderItem().getOrder().getId())
//				.userId(r.getOrderItem().getOrder().getUser().getId())
//				.userEmail(r.getOrderItem().getOrder().getUser().getEmail())
//				.productName(r.getOrderItem().getProduct().getName()).quantity(r.getReturnQuantity())
//				.reason(r.getReason().name()).status(r.getStatus().name()).refundAmount(r.getRefundAmount())
//				.requestedAt(r.getRequestedAt()).build();
//	}
//
//	@Transactional
//	public void updateReturnStatus(Long returnId, UpdateReturnStatusDto dto) {
//
//		OrderReturn r = returnRepo.findById(returnId).orElseThrow(() -> new NotFoundException("Return not found"));
//
//		r.setStatus(ReturnStatus.valueOf(dto.getStatus()));
//		r.setRefundAmount(dto.getRefundAmount());
//		r.setAdminComment(dto.getAdminComment());
//
//		returnRepo.save(r);
//	}
//
//	public PageResponseDto<AdminUserReturnResponseDto> getReturnsByUser(Long userId, int page, int size) {
//
//		userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
//
//		Page<OrderReturn> returns = returnRepo.findByOrderItem_Order_User_Id(userId,
//				PageRequest.of(page, size, Sort.by("requestedAt").descending()));
//
//		return PageResponseDto.<AdminUserReturnResponseDto>builder()
//				.content(returns.getContent().stream().map(this::mapToUserReturnDto).toList()).page(returns.getNumber())
//				.size(returns.getSize()).totalElements(returns.getTotalElements()).totalPages(returns.getTotalPages())
//				.last(returns.isLast()).build();
//	}
//
//	private AdminUserReturnResponseDto mapToUserReturnDto(OrderReturn r) {
//
//		return AdminUserReturnResponseDto.builder().returnId(r.getId())
//				.userId(r.getOrderItem().getOrder().getUser().getId())
//				.userEmail(r.getOrderItem().getOrder().getUser().getEmail())
//				.orderId(r.getOrderItem().getOrder().getId()).orderItemId(r.getOrderItem().getId())
//				.productId(r.getOrderItem().getProduct().getId()).productName(r.getOrderItem().getProduct().getName())
//				.quantity(r.getReturnQuantity()).reason(r.getReason().name()).status(r.getStatus().name())
//				.refundAmount(r.getRefundAmount()).requestedAt(r.getRequestedAt()).build();
//	}
//
//	private ReturnResponseDto mapToReturnDto(OrderReturn r) {
//
//		return ReturnResponseDto.builder().returnId(r.getId()).orderId(r.getOrderItem().getOrder().getId())
//				.orderItemId(r.getOrderItem().getId()).productId(r.getOrderItem().getProduct().getId())
//				.productName(r.getOrderItem().getProduct().getName()).quantity(r.getReturnQuantity())
//				.status(r.getStatus()).reason(r.getReason()) // ✅ FIXED
//				.refundAmount(r.getRefundAmount()).requestedAt(r.getRequestedAt()).build();
//	}
////
////	private AdminUserReturnResponseDto mapToDto(OrderReturn r) {
////
////		return AdminUserReturnResponseDto.builder().returnId(r.getId())
////				.userId(r.getOrderItem().getOrder().getUser().getId())
////				.userEmail(r.getOrderItem().getOrder().getUser().getEmail())
////				.orderId(r.getOrderItem().getOrder().getId()).orderItemId(r.getOrderItem().getId())
////				.productId(r.getOrderItem().getProduct().getId()).productName(r.getOrderItem().getProduct().getName())
////				.quantity(r.getReturnQuantity()).reason(r.getReason().name()).status(r.getStatus().name())
////				.refundAmount(r.getRefundAmount()).requestedAt(r.getRequestedAt()).build();
////	}
//
//	public List<ReturnByReasonDto> getReturnsByReason() {
//
//		return returnRepo.countByReason().stream()
//				.map(r -> ReturnByReasonDto.builder().reason(r[0].toString()).count((Long) r[1]).build()).toList();
//	}
//
//	public PageResponseDto<ReturnResponseDto> getUserReturns(Long userId, int page, int size) {
//
//		Page<OrderReturn> returns = returnRepo.findByOrderItem_Order_User_Id(userId,
//				PageRequest.of(page, size, Sort.by("createdAt").descending()));
//
//		return mapToPageDto(returns);
//	}
//
//	public PageResponseDto<ReturnResponseDto> adminSearchReturns(Long userId, ReturnStatus status, int page, int size) {
//
//		Page<OrderReturn> returns = returnRepo.search(userId, status,
//				PageRequest.of(page, size, Sort.by("createdAt").descending()));
//
//		return mapToPageDto(returns);
//	}
//
//	private PageResponseDto<ReturnResponseDto> mapToPageDto(Page<OrderReturn> page) {
//
//		List<ReturnResponseDto> content = page.getContent().stream().map(this::mapToReturnDto).toList();
//
//		return PageResponseDto.<ReturnResponseDto>builder().content(content).page(page.getNumber()).size(page.getSize())
//				.totalElements(page.getTotalElements()).totalPages(page.getTotalPages()).last(page.isLast()).build();
//	}
//
//	public PageResponseDto<ReturnResponseDto> getReturnsByStatus(ReturnStatus status, int page, int size) {
//
//		Page<OrderReturn> returns = returnRepo.findByStatus(status,
//				PageRequest.of(page, size, Sort.by("requestedAt").descending()));
//
//	return null;
//		//PageResponseDto.<ReturnResponseDto>builder().content(returns.getContent().stream()
////				.map(r -> ReturnResponseDto.builder().returnId(r.getId()).orderId(r.getOrderItem().getOrder().getId())
////						.orderItemId(r.getOrderItem().getId()).productId(r.getOrderItem().getProduct().getId())
////						.productName(r.getOrderItem().getProduct().getName()).quantity(r.getReturnQuantity())
////						.status(r.getStatus()).reason(r.getReason()).refundAmount(r.getRefundAmount())
////						.requestedAt(r.getRequestedAt()).build())
////				.toList()).page(returns.getNumber()).size(returns.getSize()).totalElements(returns.getTotalElements())
////				.totalPages(returns.getTotalPages()).last(returns.isLast()).build();
//	}
//
//	@Transactional
//	public ReorderResponseDto reorder(User user, Long orderId) {
//
//		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
//
//		if (!order.getUser().getId().equals(user.getId())) {
//			throw new UnauthorizedException("Not your order");
//		}
//
//		List<ReorderItemSuccessDto> success = new ArrayList<>();
//		List<ReorderItemFailureDto> failed = new ArrayList<>();
//
//		for (OrderItem item : order.getItems()) {
//
//			ProductInventory inventory = inventoryRepository
//					.findByProductAndLocation(item.getProduct(), order.getLocation()).orElse(null);
//
//			if (inventory == null || inventory.getStock() <= 0) {
//
//				failed.add(ReorderItemFailureDto.builder().productId(item.getProduct().getId())
//						.productName(item.getProduct().getName()).requestedQty(item.getQuantity()).availableQty(0)
//						.reason("Out of stock").build());
//				continue;
//			}
//
//			int qtyToAdd = Math.min(item.getQuantity(), inventory.getStock());
//
//		//	cartService.addOrUpdate(user, item.getProduct(), qtyToAdd);
//
//			success.add(ReorderItemSuccessDto.builder().productId(item.getProduct().getId())
//					.productName(item.getProduct().getName()).quantityAdded(qtyToAdd).build());
//
//			if (qtyToAdd < item.getQuantity()) {
//				failed.add(ReorderItemFailureDto.builder().productId(item.getProduct().getId())
//						.productName(item.getProduct().getName()).requestedQty(item.getQuantity())
//						.availableQty(inventory.getStock()).reason("Partial stock available").build());
//			}
//		}
//
//		return ReorderResponseDto.builder().success(!success.isEmpty())
//				.message(failed.isEmpty() ? "Reorder completed" : "Reorder partially completed").addedItems(success)
//				.failedItems(failed).build();
//	}
//
//	@Transactional
//	public ReturnResponseDto requestReturn(User user, ReturnRequestDto dto) {
//
//		OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
//				.orElseThrow(() -> new NotFoundException("Order item not found"));
//
//		if (!orderItem.getOrder().getUser().getId().equals(user.getId())) {
//			throw new UnauthorizedException("Not your order");
//		}
//
//		if (dto.getQuantity() <= 0 || dto.getQuantity() > orderItem.getQuantity()) {
//			throw new BadRequestException("Invalid return quantity");
//		}
//
//		OrderReturn orderReturn = OrderReturn.builder().orderItem(orderItem).user(user)
//				.returnQuantity(dto.getQuantity()).reason(dto.getReason()).status(ReturnStatus.REQUESTED)
//				.refundAmount(orderItem.getPrice() * dto.getQuantity()).requestedAt(Instant.now()).build();
//
//		orderReturnRepository.save(orderReturn);
//
//		return mapToDto(orderReturn);
//	}
//
//	private ReturnResponseDto mapToDto(OrderReturn r) {
//		return ReturnResponseDto.builder().returnId(r.getId()).orderId(r.getOrderItem().getOrder().getId())
//				.orderItemId(r.getOrderItem().getId()).productId(r.getOrderItem().getProduct().getId())
//				.productName(r.getOrderItem().getProduct().getName()).quantity(r.getReturnQuantity())
//				.reason(r.getReason()).status(r.getStatus()).refundAmount(r.getRefundAmount())
//				.requestedAt(r.getRequestedAt()).build();
//	}

	public PageResponseDto<ReturnDto> getUserReturns(User user, ReturnStatus status, int page, int size) {
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

			Page<Return> returns;
			if (status != null) {
				returns = returnRepository.findByStatus(status, pageable);
			} else {
				returns = returnRepository.findByUserId(user.getId(), pageable);
				System.err.println(returns.getNumber());
				
			}

			return PageResponseDto.<ReturnDto>builder()
					.content(returns.getContent().stream().map(this::convertToDto).collect(Collectors.toList()))
					.page(returns.getNumber()).size(returns.getSize()).totalElements(returns.getTotalElements())
					.totalPages(returns.getTotalPages()).last(returns.isLast()).build();

		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch user returns", e);
		}
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

		ReturnDto returnDto = convertToDto(returnRecord);
		dto.setReturnInfo(returnDto);
		Double price = returnRecord.getOrderItem().getPrice();
		Integer quantity = returnRecord.getOrderItem().getQuantity();
		Double total = price * quantity;
		OrderItemDto orderItemDto = OrderItemDto.builder().productId(returnRecord.getOrderItem().getProductId())
				.variantId(returnRecord.getOrderItem().getVariantId())
				.productName(returnRecord.getOrderItem().getProductName()).size(returnRecord.getOrderItem().getSize())
				.color(returnRecord.getOrderItem().getColor())
				.price(returnRecord.getOrderItem().getPrice().doubleValue())
				.quantity(returnRecord.getOrderItem().getQuantity()).total(total).build();
		dto.setOrderItem(orderItemDto);

		if (returnRecord.getRefund() != null) {
			RefundDto refundDto = new RefundDto();
			refundDto.setId(returnRecord.getRefund().getId());
			refundDto.setAmount(returnRecord.getRefund().getAmount());
			refundDto.setStatus(returnRecord.getRefund().getStatus());
			refundDto.setTransactionId(returnRecord.getRefund().getTransactionId());
			dto.setRefund(refundDto);
		}

		List<ReturnTimeline> timeline = timelineRepository
				.findByReturnRecordIdOrderByCreatedAtDesc(returnRecord.getId());
		dto.setTimeline(timeline.stream().map(this::convertToTimelineDto).collect(Collectors.toList()));

	
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
	}

	@Transactional
	public ReturnDto createReturn(Long userId, CreateReturnRequest request) {
		try {

			User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

			OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
					.orElseThrow(() -> new NotFoundException("Order item not found"));

			if (!orderItem.getOrder().getUser().getId().equals(userId)) {
				throw new UnauthorizedException("This item doesn't belong to you");
			}
			if (returnRepository.existsByOrderItemId(orderItem.getId())) {
				throw new IllegalStateException("Return already requested for this item");
			}

			Return returnRecord = new Return();
			returnRecord.setUser(user);
			returnRecord.setOrder(orderItem.getOrder());
			returnRecord.setOrderItem(orderItem);
			returnRecord.setReason(request.getReason());
			returnRecord.setReasonDescription(request.getReasonDescription());
			returnRecord.setQuantity(request.getQuantity());
			// returnRecord.setImageUrls(request.getImageUrls());

			returnRecord = returnRepository.save(returnRecord);
			return convertToDto(returnRecord);

		} catch (BadRequestException | NotFoundException | UnauthorizedException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create return request", e);
		}
	}

	@Transactional
	public ReturnDto cancelReturn(Long userId, Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (!returnRecord.getUser().getId().equals(userId)) {
				throw new UnauthorizedException("You don't have access to this return");
			}

			if (returnRecord.getStatus() != ReturnStatus.PENDING_APPROVAL) {
				throw new IllegalStateException("Cannot cancel return after it has been processed");
			}

			returnRecord.setStatus(ReturnStatus.CANCELLED);
			return convertToDto(returnRepository.save(returnRecord));

		} catch (NotFoundException | UnauthorizedException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to cancel return", e);
		}
	}

	private ReturnDto convertToDto(Return returnRecord) {
		ReturnDto dto = new ReturnDto();
		dto.setId(returnRecord.getId());
		dto.setReturnNumber(returnRecord.getReturnNumber());
		dto.setStatus(returnRecord.getStatus());
		dto.setReason(returnRecord.getReason());
		dto.setReasonDescription(returnRecord.getReasonDescription());
		dto.setQuantity(returnRecord.getQuantity());
		dto.setRefundAmount(returnRecord.getRefundAmount());
		dto.setRestockingFee(returnRecord.getRestockingFee());
		dto.setTotalRefundAmount(returnRecord.getTotalRefundAmount());
		dto.setCreatedAt(returnRecord.getCreatedAt());
		dto.setUserId(returnRecord.getUser().getId());
		dto.setOrderId(returnRecord.getOrder().getId());
		dto.setOrderItemId(returnRecord.getOrderItem().getId());
		dto.setProductName(returnRecord.getOrderItem().getProductName());
		Double price = returnRecord.getOrderItem().getPrice();
		if (price != null) {
			dto.setItemPrice(BigDecimal.valueOf(price));
		}
		
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

	@Transactional
	public ReturnDto createReturnRequest(Long userId, CreateReturnRequest request) {
		try {
			User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

			OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
					.orElseThrow(() -> new NotFoundException("Order item not found"));

			if (!orderItem.getOrder().getUser().getId().equals(userId)) {
				throw new UnauthorizedException("This item doesn't belong to you");
			}

			if (returnRepository.existsByOrderItemId(orderItem.getId())) {
				throw new IllegalStateException("Return already requested for this item");
			}

			Instant orderInstant = orderItem.getOrder().getCreatedAt();
			LocalDateTime orderDate = LocalDateTime.ofInstant(orderInstant, ZoneId.systemDefault());
			LocalDateTime returnDeadline = orderDate.plusDays(30);
			if (LocalDateTime.now().isAfter(returnDeadline)) {
				throw new IllegalStateException("Return window has expired");
			}

			if (request.getQuantity() > orderItem.getQuantity()) {
			    throw new BadRequestException(String.format(
			        "Cannot return %d items. You only purchased %d of this item",
			        request.getQuantity(), orderItem.getQuantity()));
			}

			Double itemPrice = orderItem.getPrice();
			if (itemPrice == null || itemPrice <= 0) {
			    throw new IllegalStateException("Invalid item price");
			}

			BigDecimal price = BigDecimal.valueOf(itemPrice);
			BigDecimal refundAmount = price.multiply(BigDecimal.valueOf(request.getQuantity()));

			// ===== DECLARE AND INITIALIZE VARIABLES PROPERLY =====
			double totalOrderValue = orderItem.getOrder().getTotalAmount() != null ? 
			                         orderItem.getOrder().getTotalAmount() : 0.0;
			double itemValue = itemPrice * request.getQuantity();

			// Discount calculation with safety checks
			BigDecimal discountAmount = BigDecimal.ZERO;
			if (orderItem.getOrder().getDiscountAmount() != null && 
			    orderItem.getOrder().getDiscountAmount() > 0 && 
			    totalOrderValue > 0) {
			    
			    double discountRatio = itemValue / totalOrderValue;
			    discountAmount = BigDecimal.valueOf(orderItem.getOrder().getDiscountAmount() * discountRatio);
			}

			BigDecimal taxAmount = BigDecimal.ZERO;
			if (orderItem.getOrder().getTaxAmount() != null && 
			    orderItem.getOrder().getTaxAmount() > 0 && 
			    totalOrderValue > 0) {

			    double taxRatio = itemValue / totalOrderValue;
			    taxAmount = BigDecimal.valueOf(orderItem.getOrder().getTaxAmount() * taxRatio);
			}
			Return returnRecord = new Return();
			returnRecord.setUser(user);
			returnRecord.setOrder(orderItem.getOrder());
			returnRecord.setOrderItem(orderItem);
			returnRecord.setReason(request.getReason());
			returnRecord.setReasonDescription(request.getReasonDescription());
			returnRecord.setQuantity(request.getQuantity());
			// returnRecord.setImageUrls(request.getImageUrls());
			returnRecord.setStatus(ReturnStatus.PENDING_APPROVAL);

			
			
		        returnRecord.setRefundAmount(refundAmount);
		        returnRecord.setRestockingFee(BigDecimal.ZERO); // Will be set by admin if applicable
		        returnRecord.setTotalRefundAmount(refundAmount); // Initially same as refund amount
		        returnRecord.setShippingCost(BigDecimal.ZERO); // Shipping cost handling

		        returnRecord.setRefundAmount(refundAmount);
		        returnRecord.setRestockingFee(BigDecimal.ZERO); // Admin will set if applicable
		        returnRecord.setTotalRefundAmount(refundAmount.subtract(discountAmount));
		        returnRecord.setShippingCost(calculateShippingCost(orderItem.getOrder(), request.getQuantity()));
		        log.debug("=== SAVING RETURN WITH VALUES ===");
		        log.debug("returnNumber: {}", returnRecord.getReturnNumber());
		        log.debug("status: {}", returnRecord.getStatus());
		        log.debug("quantity: {}", returnRecord.getQuantity());
		        log.debug("reason: {}", returnRecord.getReason());
		        log.debug("refundAmount: {}", returnRecord.getRefundAmount());
		        log.debug("restockingFee: {}", returnRecord.getRestockingFee());
		        log.debug("totalRefundAmount: {}", returnRecord.getTotalRefundAmount());
		        log.debug("shippingCost: {}", returnRecord.getShippingCost());
		        log.debug("createdAt: {}", returnRecord.getCreatedAt());
		        log.debug("updatedAt: {}", returnRecord.getUpdatedAt());
		        log.debug("================================");
			returnRecord = returnRepository.save(returnRecord);

			createTimelineEntry(returnRecord, "Return requested", "USER", user.getEmail());

			return convertToDto(returnRecord);
		} catch (NotFoundException | UnauthorizedException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create return request", e);
		}
	}

	
	private BigDecimal calculateShippingCost(Order order, int returnQuantity) {

	    
	    if (returnQuantity <= 3) {
	        return BigDecimal.valueOf(50); 
	    } else {
	        return BigDecimal.valueOf(100);
	    }
	}
	
	@Transactional
	public ReturnDto cancelReturnRequest(User user, Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (!returnRecord.getUser().getId().equals(user.getId())) {
				throw new UnauthorizedException("You don't have access to this return");
			}

			if (returnRecord.getStatus() != ReturnStatus.PENDING_APPROVAL) {
				throw new IllegalStateException("Cannot cancel return after it has been processed");
			}

			returnRecord.setStatus(ReturnStatus.CANCELLED);
			returnRecord = returnRepository.save(returnRecord);

			createTimelineEntry(returnRecord, "Return cancelled by customer", "USER", user.getEmail());

			return convertToDto(returnRecord);
		} catch (NotFoundException | UnauthorizedException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to cancel return", e);
		}
	}

	public ReturnTrackingDto trackReturn(User user, Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (!returnRecord.getUser().getId().equals(user.getId())) {
				throw new UnauthorizedException("You don't have access to this return");
			}

			ReturnTrackingDto trackingDto = new ReturnTrackingDto();
			trackingDto.setReturnNumber(returnRecord.getReturnNumber());
			trackingDto.setCurrentStatus(returnRecord.getStatus());

			List<ReturnTimeline> timeline = timelineRepository.findByReturnRecordIdOrderByCreatedAtDesc(returnId);

			List<ReturnTrackingDto.TrackingStep> steps = timeline.stream().map(t -> {
				ReturnTrackingDto.TrackingStep step = new ReturnTrackingDto.TrackingStep();
				step.setTitle(t.getTitle());
				step.setDescription(t.getDescription());
				step.setDate(t.getCreatedAt());
				step.setCompleted(true);
				return step;
			}).collect(Collectors.toList());

			trackingDto.setSteps(steps);

			if (returnRecord.getStatus() == ReturnStatus.APPROVED) {
				trackingDto.setEstimatedCompletionDate(LocalDateTime.now().plusDays(5));
			} else if (returnRecord.getStatus() == ReturnStatus.QC_PASSED) {
				trackingDto.setEstimatedCompletionDate(LocalDateTime.now().plusDays(2));
			}

			return trackingDto;
		} catch (NotFoundException | UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to track return", e);
		}
	}

	public List<ReturnTimelineDto> getReturnTimeline(User user, Long returnId) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (!returnRecord.getUser().getId().equals(user.getId())) {
				throw new UnauthorizedException("You don't have access to this return");
			}

			List<ReturnTimeline> timeline = timelineRepository.findByReturnRecordIdOrderByCreatedAtDesc(returnId);

			return timeline.stream().map(this::convertToTimelineDto).collect(Collectors.toList());
		} catch (NotFoundException | UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch return timeline", e);
		}
	}

	private void createTimelineEntry(Return returnRecord, String title, String role, String createdBy) {
		ReturnTimeline timeline = ReturnTimeline.builder().returnRecord(returnRecord).status(returnRecord.getStatus())
				.title(title).createdBy(createdBy).createdByRole(role).isCustomerVisible(true).isAdminVisible(true)
				.build();
		timelineRepository.save(timeline);
	}

	public PageResponseDto<ReturnDto> getAllReturns(ReturnStatus status, String search, Long productId,
			LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
		try {
			 Page<Return> returns = returnRepository.findByFilters(status, search, productId, fromDate, toDate, pageable);
			return PageResponseDto.<ReturnDto>builder()
					.content(returns.getContent().stream().map(this::convertToDto).collect(Collectors.toList()))
					.page(returns.getNumber()).size(returns.getSize()).totalElements(returns.getTotalElements())
					.totalPages(returns.getTotalPages()).last(returns.isLast()).build();
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

// GET RETURN BY ORDER ITEM ID
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
			List<Return> returns = returnRepository.findByStatus(ReturnStatus.PENDING_APPROVAL);
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

			ReturnStatus oldStatus = returnRecord.getStatus();
			returnRecord.setStatus(request.getStatus());

			returnRecord = returnRepository.save(returnRecord);

			createTimelineEntry(returnRecord, "Status updated from " + oldStatus + " to " + request.getStatus(),
					"ADMIN",  user.getEmail());

			return convertToDto(returnRecord);
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to update return status", e);
		}
	}

	@Transactional
	public ReturnDto approveReturn(Long returnId, ApproveReturnRequest request, User user) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (returnRecord.getStatus() != ReturnStatus.PENDING_APPROVAL) {
				throw new IllegalStateException(
						"Return cannot be approved in current state: " + returnRecord.getStatus());
			}

			returnRecord.setStatus(ReturnStatus.APPROVED);
			returnRecord.setApprovedAt(LocalDateTime.now());
			returnRecord.setApprovedBy(user.getId());

			Double price = returnRecord.getOrderItem().getPrice();
			BigDecimal subtotal = BigDecimal.valueOf(price * returnRecord.getQuantity());

			BigDecimal restockingFee = request.getRestockingFee() != null ? request.getRestockingFee()
					: BigDecimal.ZERO;

			returnRecord.setRefundAmount(subtotal);
			returnRecord.setRestockingFee(restockingFee);
			returnRecord.setTotalRefundAmount(subtotal.subtract(restockingFee));

			returnRecord = returnRepository.save(returnRecord);

			createTimelineEntry(returnRecord, "Return approved by admin", "ADMIN", user.getEmail());

			return convertToDto(returnRecord);
		} catch (NotFoundException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to approve return", e);
		}
	}

	@Transactional
	public ReturnDto rejectReturn(Long returnId, RejectReturnRequest request, User user) {
		try {
			Return returnRecord = returnRepository.findById(returnId)
					.orElseThrow(() -> new NotFoundException("Return not found"));

			if (returnRecord.getStatus() != ReturnStatus.PENDING_APPROVAL) {
				throw new IllegalStateException(
						"Return cannot be rejected in current state: " + returnRecord.getStatus());
			}

			returnRecord.setStatus(ReturnStatus.REJECTED);
			returnRecord.setRejectedAt(LocalDateTime.now());
			returnRecord.setRejectionReason(request.getRejectionReason());

			returnRecord = returnRepository.save(returnRecord);

			createTimelineEntry(returnRecord, "Return rejected: " + request.getRejectionReason(), "ADMIN",
					user.getEmail());

			return convertToDto(returnRecord);
		} catch (NotFoundException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to reject return", e);
		}
	}
	
	public ReturnDashboardStats getReturnDashboardStats() {
		try {
			ReturnDashboardStats stats = new ReturnDashboardStats();

			stats.setTotalReturns(returnRepository.count());
			stats.setPendingApproval(returnRepository.countByStatus(ReturnStatus.PENDING_APPROVAL));
			stats.setApproved(returnRepository.countByStatus(ReturnStatus.APPROVED));
			stats.setRejected(returnRepository.countByStatus(ReturnStatus.REJECTED));
			stats.setProcessing(returnRepository.countByStatusIn(
					List.of(ReturnStatus.PENDING_PICKUP, ReturnStatus.PICKUP_SCHEDULED, ReturnStatus.QC_PENDING)));
			stats.setCompleted(returnRepository.countByStatus(ReturnStatus.REFUND_COMPLETED));

// Get returns by status map
			stats.setReturnsByStatus(returnRepository.countByStatusGrouped());

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
	        // Get raw data
	        List<Object[]> results = returnRepository.getReturnReasonAnalyticsRaw(fromDate, toDate);
	        
	        // Calculate total for percentages
	        Long totalCount = results.stream()
	                .map(row -> (Long) row[1])
	                .reduce(0L, Long::sum);
	        
	        // Convert to DTOs - FIXED: Use stream() with map() correctly
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
	        
	        // Manual pagination
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
	    try {
	        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
	        Page<Return> returns = returnRepository.findByUserId(userId, pageable);
	        
	        return PageResponseDto.<AdminUserReturnResponseDto>builder()
	                .content(returns.getContent().stream()
	                        .map(this::convertToAdminUserReturnResponseDto)
	                        .collect(Collectors.toList()))
	                .page(returns.getNumber())
	                .size(returns.getSize())
	                .totalElements(returns.getTotalElements())
	                .totalPages(returns.getTotalPages())
	                .last(returns.isLast())
	                .build();
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to fetch returns for user: " + userId, e);
	    }
	}

	private AdminUserReturnResponseDto convertToAdminUserReturnResponseDto(Return returnRecord) {
	    AdminUserReturnResponseDto dto = new AdminUserReturnResponseDto();
	    dto.setReturnId(returnRecord.getId());
	    dto.setReturnNumber(returnRecord.getReturnNumber());
	    dto.setStatus(returnRecord.getStatus());
	    dto.setReason(returnRecord.getReason());
	    dto.setQuantity(returnRecord.getQuantity());
	    dto.setRefundAmount(returnRecord.getTotalRefundAmount());
	    dto.setCreatedAt(returnRecord.getCreatedAt());
	    dto.setOrderId(returnRecord.getOrder().getId());
	   dto.setProductName(returnRecord.getOrderItem().getProductName());
	    dto.setProductPrice(returnRecord.getOrderItem().getPrice());
	     dto.setUserEmail(returnRecord.getUser().getEmail());
	 //   dto.setUserName(returnRecord.getUser().getUsername());
	    
	    return dto;
	}
	
	
//private ReturnDto convertToDto(Return returnRecord) {
//ReturnDto dto = new ReturnDto();
//dto.setId(returnRecord.getId());
//dto.setReturnNumber(returnRecord.getReturnNumber());
//dto.setStatus(returnRecord.getStatus());
//dto.setReason(returnRecord.getReason());
//dto.setReasonDescription(returnRecord.getReasonDescription());
//dto.setQuantity(returnRecord.getQuantity());
//dto.setRefundAmount(returnRecord.getRefundAmount());
//dto.setRestockingFee(returnRecord.getRestockingFee());
//dto.setTotalRefundAmount(returnRecord.getTotalRefundAmount());
//dto.setCreatedAt(returnRecord.getCreatedAt());
//dto.setUserId(returnRecord.getUser().getId());
//dto.setUserName(returnRecord.getUser().getUsername());
//dto.setOrderId(returnRecord.getOrder().getId());
//dto.setOrderNumber(returnRecord.getOrder().getOrderNumber());
//dto.setOrderItemId(returnRecord.getOrderItem().getId());
//dto.setProductName(returnRecord.getOrderItem().getProductName());
//
//Double price = returnRecord.getOrderItem().getPrice();
//if (price != null) {
//dto.setItemPrice(BigDecimal.valueOf(price));
//}
//
//dto.setImageUrls(returnRecord.getImageUrls());
//return dto;
//}

//private ReturnDetailDto convertToReturnDetailDto(Return returnRecord) {
//ReturnDetailDto dto = new ReturnDetailDto();
//dto.setReturnInfo(convertToDto(returnRecord));
//
//// Add timeline
//dto.setTimeline(timelineRepository.findByReturnRecordIdOrderByCreatedAtDesc(returnRecord.getId())
//.stream()
//.map(timeline -> {
//com.project.backend.ResponseDto.ReturnTimelineDto timelineDto = 
//new com.project.backend.ResponseDto.ReturnTimelineDto();
//timelineDto.setId(timeline.getId());
//timelineDto.setReturnId(timeline.getReturnRecord().getId());
//timelineDto.setStatus(timeline.getStatus().toString());
//timelineDto.setTitle(timeline.getTitle());
//timelineDto.setDescription(timeline.getDescription());
//timelineDto.setCreatedAt(timeline.getCreatedAt());
//timelineDto.setCreatedBy(timeline.getCreatedBy());
//timelineDto.setCreatedByRole(timeline.getCreatedByRole());
//return timelineDto;
//})
//.collect(Collectors.toList()));
//
//dto.setImageUrls(returnRecord.getImageUrls());
//return dto;
//}

//private void createTimelineEntry(Return returnRecord, String title, String role, String createdBy) {
//com.project.backend.entity.ReturnTimeline timeline = 
//com.project.backend.entity.ReturnTimeline.builder()
//.returnRecord(returnRecord)
//.status(returnRecord.getStatus())
//.title(title)
//.createdBy(createdBy)
//.createdByRole(role)
//.isCustomerVisible(true)
//.isAdminVisible(true)
//.build();
//timelineRepository.save(timeline);
//}

}
