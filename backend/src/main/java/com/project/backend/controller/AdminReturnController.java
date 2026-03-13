package com.project.backend.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AdminUserReturnResponseDto;
import com.project.backend.ResponseDto.ProductReturnStatsDto;
import com.project.backend.ResponseDto.ReturnDashboardStats;
import com.project.backend.ResponseDto.ReturnDetailDto;
import com.project.backend.ResponseDto.ReturnDto;
import com.project.backend.ResponseDto.ReturnReasonStatsDto;
import com.project.backend.entity.ReturnStatus;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.ApproveReturnRequest;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.RejectReturnRequest;
import com.project.backend.requestDto.UpdateReturnStatusRequest;
import com.project.backend.service.AdminReturnService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Returns & Refund")
public class AdminReturnController {

	private final AdminReturnService service;
	 private final UserRepository userRepository;
	
	private User getCurrentUser(Authentication auth) {
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
	}
	
	//return
	@Operation(summary = "Get all returns", security = @SecurityRequirement(name = "Bearer Authentication"))
	  @GetMapping("/returns")
	    public ResponseEntity<PageResponseDto<ReturnDto>> getAllReturns(
	            @RequestParam(required = false) ReturnStatus status,
	            @RequestParam(required = false) String search,
	            @RequestParam(required = false) Long productId,
	            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
	            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
	            Pageable pageable) {
	        return ResponseEntity.ok(service.getAllReturns(status, search, productId, fromDate, toDate, pageable));
	    }

	@Operation(summary = "Get  return by id", security = @SecurityRequirement(name = "Bearer Authentication"))
	    @GetMapping("/returns/{returnId}")
	    public ResponseEntity<ReturnDetailDto> getReturnById(@PathVariable Long returnId) {
	        return ResponseEntity.ok(service.getReturnById(returnId));
	    }

	@Operation(summary = "Getreturn by orerItemId", security = @SecurityRequirement(name = "Bearer Authentication"))
	    @GetMapping("/returns/by-order-item/{orderItemId}")
	    public ResponseEntity<ReturnDto> getReturnByOrderItem(@PathVariable Long orderItemId) {
	        return ResponseEntity.ok(service.getReturnByOrderItem(orderItemId));
	    }

	@Operation(summary = "pendng returns", security = @SecurityRequirement(name = "Bearer Authentication"))
	    @GetMapping("/returns/pending")
	    public ResponseEntity<List<ReturnDto>> getPendingReturns() {
	        return ResponseEntity.ok(service.getPendingReturns());
	    }

	@Operation(summary = "update return status", security = @SecurityRequirement(name = "Bearer Authentication"))
	    @PutMapping("/returns/{returnId}/status")
	    public ResponseEntity<ReturnDto> updateReturnStatus(
	            @PathVariable Long returnId,
	            @Valid @RequestBody UpdateReturnStatusRequest request,
	           Authentication auth) { User user = getCurrentUser(auth);
	        return ResponseEntity.ok(service.updateReturnStatus(returnId, request, user));
	    }

	@Operation(summary = "approve returns", security = @SecurityRequirement(name = "Bearer Authentication"))
	    @PostMapping("/returns/{returnId}/approve")
	    public ResponseEntity<ReturnDto> approveReturn(
	            @PathVariable Long returnId,
	            @Valid @RequestBody ApproveReturnRequest request,
	           Authentication auth) { User user = getCurrentUser(auth);
	        return ResponseEntity.ok(service.approveReturn(returnId, request, user));
	    }

	@Operation(summary = "reject returns", security = @SecurityRequirement(name = "Bearer Authentication"))
	    @PostMapping("/returns/{returnId}/reject")
	    public ResponseEntity<ReturnDto> rejectReturn(
	            @PathVariable Long returnId,
	            @Valid @RequestBody RejectReturnRequest request,
	           Authentication auth) { User user = getCurrentUser(auth);
	        return ResponseEntity.ok(service.rejectReturn(returnId, request, user));
	    }
	
	
	 
	 @Operation(summary = "Get all returns of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
	 @GetMapping("/dashboard/return-stats")
	    public ResponseEntity<ReturnDashboardStats> getReturnDashboardStats() {
	        return ResponseEntity.ok(service.getReturnDashboardStats());
	    }
	 
	 
	 @Operation(summary = "Get all returns of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
	 @GetMapping("/analytics/top-returned-products")
	 public ResponseEntity<PageResponseDto<ProductReturnStatsDto>> getTopReturnedProducts(
	         @RequestParam(defaultValue = "0") int page,
	         @RequestParam(defaultValue = "10") int size) {
	     return ResponseEntity.ok(service.getTopReturnedProducts(page, size));
	 }

	 
	 @Operation(summary = "Get all returns of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
	 @GetMapping("/analytics/return-reasons")
	 public ResponseEntity<PageResponseDto<ReturnReasonStatsDto>> getReturnReasonAnalytics(
	         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
	         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
	         @RequestParam(defaultValue = "0") int page,
	         @RequestParam(defaultValue = "10") int size) {
	     return ResponseEntity.ok(service.getReturnReasonAnalytics(fromDate, toDate, page, size));
	 }
	 
	 
	 
	 @Operation(summary = "Get all returns of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
	 @GetMapping("/user/{userId}")
	 public ResponseEntity<PageResponseDto<AdminUserReturnResponseDto>> getUserReturns(
	         @PathVariable Long userId,
	         @RequestParam(defaultValue = "0") int page, 
	         @RequestParam(defaultValue = "10") int size) {

	     return ResponseEntity.ok(service.getReturnsByUser(userId, page, size));
	 }
	 
//
//	    @PostMapping("/returns/{returnId}/images")
//	    public ResponseEntity<List<String>> uploadReturnImages(
//	            @PathVariable Long returnId,
//	            @RequestParam("images") List<MultipartFile> images) {
//	        return ResponseEntity.ok(service.uploadReturnImages(returnId, images));
//	    }

	
	
	
	
	
	
	

//	@Operation(summary = "Get all returns", security = @SecurityRequirement(name = "Bearer Authentication"))
//	@GetMapping
//	public ResponseEntity<?> getAll(@RequestParam int page, @RequestParam int size) {
//
//		return ResponseEntity.ok(service.getAllReturns(page, size));
//	}
//
//	@Operation(summary = "Update return status", security = @SecurityRequirement(name = "Bearer Authentication"))
//	@PutMapping("/{returnId}")
//	public ResponseEntity<Void> update(@PathVariable Long returnId, @RequestBody UpdateReturnStatusDto dto) {
//
//		service.updateReturnStatus(returnId, dto);
//		return ResponseEntity.ok().build();
//	}
//
//	@Operation(summary = "Get all returns of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
//	@GetMapping("/user/{userId}")
//	public ResponseEntity<PageResponseDto<AdminUserReturnResponseDto>> getUserReturns(@PathVariable Long userId,
//			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
//
//		return ResponseEntity.ok(service.getReturnsByUser(userId, page, size));
//	}

}
