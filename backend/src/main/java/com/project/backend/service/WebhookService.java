package com.project.backend.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.entity.Order;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.Return;
import com.project.backend.entity.ReturnStatus;
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
    private final AdminReturnService adminReturnService;
    private final WarehouseService warehouseService;
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
    String incomingStatusStr = (String) payload.get("current_status");

    if (awb == null || incomingStatusStr == null) {
        log.warn("Invalid webhook payload: {}", payload);
        return;
    }

    Return ret = returnRepository.findByTrackingId(awb).orElse(null);

    if (ret == null) {
        log.warn("Unknown AWB: {}", awb);
        return;
    }

    String normalized = normalizeStatus(incomingStatusStr);

    ReturnStatus currentStatus = ReturnStatus.valueOf(ret.getStatus());
    ReturnStatus incomingStatus = mapToReturnStatus(normalized);

    if (incomingStatus == null) {
        log.warn("Unknown webhook status: {}", normalized);
        return;
    }

    // 🔥 ORDER CHECK
    if (!isForwardTransition(currentStatus, incomingStatus)) {
        log.warn("Out-of-order webhook ignored. current={}, incoming={}",
                currentStatus, incomingStatus);
        return;
    }

    ret.setUpdatedAt(LocalDateTime.now());

    // 🔥 ENUM SWITCH (IMPORTANT)
    switch (incomingStatus) {

        case PICKED_UP:
            ret.setStatus("PICKED_UP");
            break;

        case IN_TRANSIT:
            ret.setStatus("IN_TRANSIT");
            break;

        case RECEIVED:
            ret.setStatus("RECEIVED");

            // 🔥 INVENTORY
            if (!Boolean.TRUE.equals(ret.getInventoryRestocked())) {
                warehouseService.restock(ret);
                ret.setInventoryRestocked(true);
            }

            if (!refundRepository.existsByReturnRequestId(ret.getId())) {
                try {
                    ret.setStatus("REFUND_PENDING");

                    adminReturnService.processRefund(ret.getId());

                    ret.setStatus("REFUNDED");

                } catch (Exception e) {
                    log.error("Refund failed for returnId={}", ret.getId(), e);
                    ret.setStatus("FAILED");
                }
            }

            break;

        case FAILED:
            ret.setStatus("FAILED");
            break;
    }

    returnRepository.save(ret);
}
private String normalizeStatus(String status) {

    String s = status.toUpperCase();

    if (s.contains("PICKED")) return "PICKED_UP";
    if (s.contains("TRANSIT")) return "IN_TRANSIT";
    if (s.contains("DELIVERED")) return "DELIVERED";
    if (s.contains("RTO")) return "RTO";

    return s;
}

private boolean isForwardTransition(ReturnStatus current, ReturnStatus incoming) {

    if (current == null) return true;

    int curr = STATUS_ORDER.getOrDefault(current, 0);
    int inc = STATUS_ORDER.getOrDefault(incoming, 0);

    return inc >= curr;
}
private static final Map<ReturnStatus, Integer> STATUS_ORDER = Map.of(
        ReturnStatus.PICKUP_SCHEDULED, 1,
        ReturnStatus.PICKED_UP, 2,
        ReturnStatus.IN_TRANSIT, 3,
        ReturnStatus.RECEIVED, 4
);
private ReturnStatus mapToReturnStatus(String status) {

    String s = status.toUpperCase();

    if (s.contains("PICKED")) return ReturnStatus.PICKED_UP;
    if (s.contains("TRANSIT")) return ReturnStatus.IN_TRANSIT;
    if (s.contains("DELIVERED")) return ReturnStatus.RECEIVED;
    if (s.contains("RTO") || s.contains("FAILED")) return ReturnStatus.FAILED;

    return null;
}
}
