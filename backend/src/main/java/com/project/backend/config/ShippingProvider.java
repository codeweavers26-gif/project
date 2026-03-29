package com.project.backend.config;

import com.project.backend.ResponseDto.ServiceabilityResponse;
import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.ResponseDto.TrackingResponse;
import com.project.backend.entity.Order;

public interface ShippingProvider {

    ShipmentResponse createShipment(Order order);

    TrackingResponse trackShipment(String trackingId);

    void cancelShipment(String shipmentId);
    ShipmentResponse assignCourier(String shipmentId);

    ServiceabilityResponse checkServiceability(
        String pickupPincode,
        String deliveryPincode,
        double weight,
        boolean cod
);

ShipmentResponse createReturnShipment(Order order) ;
}