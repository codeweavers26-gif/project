package com.project.backend.service;

import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.config.RetryUtil;
import com.project.backend.config.ShippingFactory;
import com.project.backend.entity.Order;
import com.project.backend.entity.Shipment;
import com.project.backend.entity.ShippingProviderType;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingAsyncService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingFactory shippingFactory;
    private final ShiprocketService shiprocketService;

    @Async
    public void triggerShippingAsync(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        log.info("Triggering shipping for orderId={}", order.getId());

        try {
            ShipmentResponse shipment = RetryUtil.executeWithRetry(
                    () -> shippingFactory
                            .getProvider(ShippingProviderType.SHIPROCKET)
                            .createShipment(order),
                    3);

            // Update order with shipment ID
            order.setShippingProvider(ShippingProviderType.SHIPROCKET.name());
            order.setShipmentId(shipment.getShipmentId());
            order.setShippingStatus("CREATED");
            orderRepository.save(order);
            log.info("Shipment created for orderId={}, shipmentId={}", order.getId(), shipment.getShipmentId());

            // Assign courier (AWB)
            ShipmentResponse assigned = shiprocketService.assignCourier(shipment.getShipmentId());
            log.info("Assigned courier response: {}", assigned);

            order.setTrackingId(assigned.getTrackingId());
            order.setShippingProvider("SHIPROCKET");
            order.setShippingStatus("AWB_ASSIGNED");
            orderRepository.save(order);

            Shipment shipmentEntity = Shipment.builder()
                    .order(order)
                    .trackingId(assigned.getTrackingId())
                    .shippingStatus("AWB_ASSIGNED")
                    .courierName("SHIPROCKET")
                    .warehouse(order.getWarehouse())
                    .build();
            shipmentRepository.save(shipmentEntity);

            log.info("Shipping fully completed for orderId={}, AWB={}", order.getId(), assigned.getTrackingId());

        } catch (Exception e) {
            log.error("Shiprocket failed for orderId={}: {}", order.getId(), e.getMessage(), e);
            order.setShippingStatus("FAILED");
            orderRepository.save(order);
        }
    }
}
