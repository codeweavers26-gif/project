package com.project.backend.config;

import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.ResponseDto.TrackingResponse;
import com.project.backend.entity.Order;

public interface ShippingProvider {

    ShipmentResponse createShipment(Order order);

    TrackingResponse trackShipment(String trackingId);

    void cancelShipment(String shipmentId);
}