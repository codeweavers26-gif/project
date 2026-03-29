package com.project.backend.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.entity.Order;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.Return;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.RefundRepository;
import com.project.backend.repository.ReturnRepository;
import com.project.backend.repository.ReturnTimelineRepository;
import com.project.backend.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {
	private final ReturnRepository returnRepository;
	private final UserRepository userRepository;
	private final CartService cartService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ReturnTimelineRepository timelineRepository;
	private final RefundRepository refundRepository;
@Transactional
public void processOrderWebhook(Map<String, Object> payload) {

    try {
        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        if (data == null) {
            log.error("Invalid webhook payload: {}", payload);
            return;
        }

        String orderIdStr = (String) data.get("order_id");
        String status = (String) data.get("current_status");
        String awb = (String) data.get("awb");

        Long orderId = Long.valueOf(orderIdStr);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        log.info("Updating order {} with status {}", orderId, status);

        updateOrderStatus(order, status, awb);

        orderRepository.save(order);

    } catch (Exception e) {
        log.error("Error processing webhook", e);
    }
}

private void updateOrderStatus(Order order, String shiprocketStatus, String awb) {

    order.setTrackingId(awb);

    switch (shiprocketStatus.toUpperCase()) {

        case "NEW":
        case "AWB_ASSIGNED":
            order.setShippingStatus("CREATED");
            break;

        case "PICKED_UP":
        case "IN_TRANSIT":
            order.setShippingStatus("IN_TRANSIT");
            order.setStatus(OrderStatus.SHIPPED);
            break;

        case "OUT_FOR_DELIVERY":
            order.setShippingStatus("OUT_FOR_DELIVERY");
            break;

        case "DELIVERED":
            order.setShippingStatus("DELIVERED");
            order.setStatus(OrderStatus.DELIVERED);
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            break;

        case "CANCELLED":
        case "RTO":
            order.setShippingStatus("FAILED");
            order.setStatus(OrderStatus.CANCELLED);
            break;

        default:
            log.warn("Unknown Shiprocket status: {}", shiprocketStatus);
    }
}


@Transactional
public void processReturnWebhook(Map<String, Object> payload) {
String awb = (String) payload.get("awb_code");
    String status = (String) payload.get("current_status");

    Return ret = returnRepository.findByTrackingId(awb)
            .orElseThrow();

    ret.setStatus(status); 
    ret.setUpdatedAt(LocalDateTime.now());

    if ("DELIVERED".equalsIgnoreCase(status)) {
      //  paymentService.refund(ret.getOrderId());
        ret.setStatus("REFUNDED");
    }

    returnRepository.save(ret);
}
}
