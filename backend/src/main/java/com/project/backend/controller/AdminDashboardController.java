package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AdminDashboardResponse;
import com.project.backend.ResponseDto.CustomerSummaryDto;
import com.project.backend.ResponseDto.OrderStatusCountDto;
import com.project.backend.ResponseDto.OrdersSummaryDto;
import com.project.backend.ResponseDto.ReturnByReasonDto;
import com.project.backend.ResponseDto.ReturnTrendDto;
import com.project.backend.ResponseDto.RevenueSummaryDto;
import com.project.backend.ResponseDto.TopReturnedProductDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "APIs for admin analytics and system metrics")
public class AdminDashboardController {

	private final AdminDashboardService dashboardService;

	@Operation(summary = "Get Admin Dashboard Metrics", description = "Returns overall platform statistics like sales, orders, users, inventory, and cart data.", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Dashboard metrics fetched successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
			@ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN users can access this") })
	@GetMapping("/dashboard")

	public ResponseEntity<AdminDashboardResponse> getDashboard() {
		return ResponseEntity.ok(dashboardService.getDashboardMetrics());
	}

	@Operation(summary = "Get total orders summary", description = "Returns total number of orders for today, this week, and this month", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Orders summary fetched successfully") })
	@GetMapping("/orders-summary")
	public OrdersSummaryDto getOrdersSummary() {
		return dashboardService.getOrdersSummary();
	}

	@Operation(summary = "Get order counts by status", description = "Returns count of orders grouped by status (Pending, Shipped, Cancelled, Delivered)", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Order status counts fetched successfully") })
	@GetMapping("/order-status-count")
	public OrderStatusCountDto getOrderStatusCounts() {
		return dashboardService.getOrderStatusCounts();
	}

	@Operation(summary = "Get new customers summary", description = "Returns number of new customers added today, this week, and this month", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Customer summary fetched successfully") })
	@GetMapping("/new-customers")
	public CustomerSummaryDto getNewCustomers() {
		return dashboardService.getNewCustomers();
	}

//	@Operation(summary = "Get admin alerts", description = "Returns alerts such as low stock products, failed payments, and return requests", security = {
//			@SecurityRequirement(name = "Bearer Authentication") })
//	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Alerts fetched successfully") })
//	@GetMapping("/alerts")
//	public AlertSummaryDto getAlerts() {
//		return dashboardService.getAlerts();
//	}
//
//	@Operation(summary = "Get shipping and logistics summary", description = "Returns shipment counts like ready to ship, in transit, and delivered today", security = {
//			@SecurityRequirement(name = "Bearer Authentication") })
//	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Shipping summary fetched successfully") })
//	@GetMapping("/shipping-summary")
//	public ShippingSummaryDto getShippingSummary() {
//		return dashboardService.getShippingSummary();
//	}

	@Operation(summary = "Get revenue and tax summary", description = "Returns revenue metrics including today/month revenue, tax collected, COD vs prepaid orders", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Revenue summary fetched successfully") })
	@GetMapping("/revenue-summary")
	public RevenueSummaryDto getRevenueSummary() {
		return dashboardService.getRevenueSummary();
	}
	@Operation(summary = "Get Admin Dashboard return by reason", description = "Get Admin Dashboard return  by reason.", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

	@GetMapping("/return/by-reason")
	public ResponseEntity<PageResponseDto<ReturnByReasonDto>> byReason(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(dashboardService.getReturnsByReason(page, size));
	}
	@Operation(summary = "Get Admin Dashboard top product", description = "Get Admin Dashboard top product.", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

	@GetMapping("/return/top-products")
	public ResponseEntity<PageResponseDto<TopReturnedProductDto>> topProducts(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(dashboardService.getTopReturnedProducts(page, size));
	}
	@Operation(summary = "Get Admin Dashboard return trend", description = "Get Admin Dashboard return trenda.", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

	@GetMapping("/return/trend")
	public ResponseEntity<PageResponseDto<ReturnTrendDto>> trend(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(dashboardService.getReturnTrend(page, size));
	}
}
