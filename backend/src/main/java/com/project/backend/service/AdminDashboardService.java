package com.project.backend.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AdminDashboardResponse;
import com.project.backend.ResponseDto.CustomerSummaryDto;
import com.project.backend.ResponseDto.OrderStatusCountDto;
import com.project.backend.ResponseDto.OrdersSummaryDto;
import com.project.backend.ResponseDto.ReturnByReasonDto;
import com.project.backend.ResponseDto.ReturnTrendDto;
import com.project.backend.ResponseDto.RevenueSummaryDto;
import com.project.backend.ResponseDto.TopReturnedProductDto;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.Role;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.OrderReturnRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
	private final OrderReturnRepository returnRepo;
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
    
    public OrdersSummaryDto getOrdersSummary() {
    	Instant now = Instant.now();

    	Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);

    	Instant startOfWeek = now
    	        .atZone(ZoneId.systemDefault())
    	        .with(DayOfWeek.MONDAY)
    	        .truncatedTo(ChronoUnit.DAYS)
    	        .toInstant();

    	Instant startOfMonth = now
    	        .atZone(ZoneId.systemDefault())
    	        .withDayOfMonth(1)
    	        .truncatedTo(ChronoUnit.DAYS)
    	        .toInstant();

        return new OrdersSummaryDto(
                orderRepository.countByCreatedAtAfter(startOfDay),
                orderRepository.countByCreatedAtAfter(startOfWeek),
                orderRepository.countByCreatedAtAfter(startOfMonth)
        );
    }

    // 2️⃣ Orders by Status
    public OrderStatusCountDto getOrderStatusCounts() {
        return new OrderStatusCountDto(
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.SHIPPED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                orderRepository.countByStatus(OrderStatus.DELIVERED),
                orderRepository.countByStatus(OrderStatus.PAID),
                orderRepository.countByStatus(OrderStatus.PLACED),
                orderRepository.countByStatus(OrderStatus.RETURN_REQUESTED)
        );
    }

    // 3️⃣ New Customers
    public CustomerSummaryDto getNewCustomers() {
    	Instant now = Instant.now();

    	Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);

    	Instant startOfWeek = now
    	        .atZone(ZoneId.systemDefault())
    	        .with(DayOfWeek.MONDAY)
    	        .truncatedTo(ChronoUnit.DAYS)
    	        .toInstant();

    	Instant startOfMonth = now
    	        .atZone(ZoneId.systemDefault())
    	        .withDayOfMonth(1)
    	        .truncatedTo(ChronoUnit.DAYS)
    	        .toInstant();

        return new CustomerSummaryDto(
                userRepository.countByCreatedAtAfter(startOfDay),
                userRepository.countByCreatedAtAfter(startOfWeek),
                userRepository.countByCreatedAtAfter(startOfMonth)
        );
    }

//    // 4️⃣ Alerts
//    public AlertSummaryDto getAlerts() {
//        return new AlertSummaryDto(
//                productRepository.countByStockLessThan(10), // low stock threshold
//                paymentRepository.countByStatus("FAILED"),
//                orderRepository.countByStatus("RETURN_REQUESTED")
//        );
//    }

//    // 5️⃣ Shipping Summary
//    public ShippingSummaryDto getShippingSummary() {
//        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
//
//        return new ShippingSummaryDto(
//                shipmentRepository.countByStatus("READY_TO_SHIP"),
//                shipmentRepository.countByStatus("IN_TRANSIT"),
//                shipmentRepository.countByStatusAndDeliveredAtAfter("DELIVERED", startOfDay)
//        );
//    }

    public RevenueSummaryDto getRevenueSummary() {
        Instant now = Instant.now();

        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);

        Instant startOfMonth = now
                .atZone(ZoneId.systemDefault())
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();

        return new RevenueSummaryDto(
                orderRepository.sumTotalAmountByCreatedAtAfter(startOfDay),
                orderRepository.sumTotalAmountByCreatedAtAfter(startOfMonth),
                orderRepository.sumTaxAmountByCreatedAtAfter(startOfMonth),
                orderRepository.countByPaymentMethod(PaymentMethod.COD),
                orderRepository.countByPaymentMethod(PaymentMethod.PREPAID)
        );
    }

    public PageResponseDto<ReturnByReasonDto> getReturnsByReason(int page, int size) {

        Page<Object[]> result =
                returnRepo.countByReason(PageRequest.of(page, size));

        return PageResponseDto.<ReturnByReasonDto>builder()
                .content(
                    result.getContent().stream()
                        .map(r -> ReturnByReasonDto.builder()
                                .reason(r[0].toString())
                                .count((Long) r[1])
                                .build())
                        .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
    public PageResponseDto<TopReturnedProductDto> getTopReturnedProducts(
            int page, int size) {

        Page<Object[]> result =
                returnRepo.topReturnedProducts(
                    PageRequest.of(page, size)
                );

        return PageResponseDto.<TopReturnedProductDto>builder()
                .content(
                    result.getContent().stream()
                        .map(r -> TopReturnedProductDto.builder()
                                .productId((Long) r[0])
                                .productName((String) r[1])
                                .returnCount((Long) r[2])
                                .build())
                        .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
    public PageResponseDto<ReturnTrendDto> getReturnTrend(
            int page, int size) {

        Page<Object[]> result =
                returnRepo.returnTrend(
                    PageRequest.of(page, size)
                );

        return PageResponseDto.<ReturnTrendDto>builder()
                .content(
                    result.getContent().stream()
                        .map(r -> ReturnTrendDto.builder()
                                .date(((java.sql.Date) r[0]).toLocalDate())
                                .count((Long) r[1])
                                .build())
                        .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    
}
