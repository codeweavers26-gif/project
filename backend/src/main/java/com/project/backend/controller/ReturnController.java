package com.project.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.EligibilityCheckDto;
import com.project.backend.ResponseDto.EligibleReturnItemDto;
import com.project.backend.ResponseDto.ReturnDetailDto;
import com.project.backend.ResponseDto.ReturnDto;
import com.project.backend.ResponseDto.ReturnTimelineDto;
import com.project.backend.ResponseDto.ReturnTrackingDto;
import com.project.backend.entity.ReturnStatus;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.CreateReturnRequest;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ReturnEligibilityRequest;
import com.project.backend.service.AdminReturnService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Tag(name = "Customer - Returns")
public class ReturnController {

    private final AdminReturnService returnService;

    private final UserRepository userRepository;

	private User getCurrentUser(Authentication auth) {
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
	}

	@GetMapping("/returns")
  @Operation(summary = "Request product return",
  security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<PageResponseDto<ReturnDto>> getMyReturns(
            @RequestParam(required = false) ReturnStatus status,
            Authentication auth,
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.getUserReturns(user, status, page, size));
    }

    @GetMapping("/returns/{returnId}")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ReturnDetailDto> getReturnDetails(
            @PathVariable Long returnId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.getReturnDetails(user, returnId));
    }

    @GetMapping("/orders/{orderId}/eligible-items")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<List<EligibleReturnItemDto>> getEligibleItemsForReturn(
            @PathVariable Long orderId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.getEligibleItems(user.getId(), orderId));
    }

    @PostMapping("/returns/check-eligibility")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<EligibilityCheckDto> checkReturnEligibility(
            @Valid @RequestBody ReturnEligibilityRequest request,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.checkReturnEligibility(user.getId(), request));
    }

    @PostMapping("/returns")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ReturnDto> createReturnRequest(
            @Valid @RequestBody CreateReturnRequest request,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.createReturnRequest(user.getId(), request));
    }

    @PutMapping("/returns/{returnId}/cancel")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ReturnDto> cancelReturnRequest(
            @PathVariable Long returnId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.cancelReturnRequest(user, returnId));
    }

    @GetMapping("/returns/{returnId}/track")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ReturnTrackingDto> trackReturn(
            @PathVariable Long returnId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.trackReturn(user, returnId));
    }

    @GetMapping("/returns/{returnId}/timeline")

    @Operation(summary = "Request product return",
    security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<List<ReturnTimelineDto>> getReturnTimeline(
            @PathVariable Long returnId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(returnService.getReturnTimeline(user, returnId));
    }
}
