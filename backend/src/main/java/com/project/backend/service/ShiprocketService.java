package com.project.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.project.backend.ResponseDto.ServiceabilityResponse;
import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.ResponseDto.TrackingResponse;
import com.project.backend.config.ShippingProvider;

import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.OrderRepository;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketService implements ShippingProvider {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
private String token;
private LocalDateTime tokenExpiry;
    private static final String BASE_URL = "https://apiv2.shiprocket.in/v1/external";

public void authenticate() {
    String url = "https://apiv2.shiprocket.in/v1/external/auth/login";
    
    Map<String, String> request = new HashMap<>();
    request.put("email", "yadavpiyush8302@gmail.com");
    request.put("password", "VWzIzOV14xyGhkWSYbHo&LF!bXqaN^D8");
    
   try {
        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Shiprocket auth failed");
        }

        Map<String, Object> body = response.getBody();

        log.debug("Shiprocket auth response: {}", body);

        if (body.containsKey("error")) {
            throw new RuntimeException("Shiprocket auth error: " + body.get("error"));
        }

        String newToken = (String) body.get("token");

        if (newToken == null) {
            throw new RuntimeException("Token not found in response");
        }

        this.token = newToken;

        this.tokenExpiry = LocalDateTime.now().plusHours(9);

        log.info("Shiprocket token refreshed");

    } catch (Exception e) {
        log.error("Shiprocket authentication failed", e);
        throw new RuntimeException("Failed to authenticate with Shiprocket", e);
    }
}

public String getValidToken() {

    if (token == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
        log.info("Token expired or missing, re-authenticating...");
        authenticate();
    }

    return token;
}
@Override
public ShipmentResponse createShipment(Order order) {

    String url = BASE_URL + "/orders/create/adhoc";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getValidToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = buildRequest(order);

    log.info("Shiprocket request: {}", body);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response =
            restTemplate.postForEntity(url, entity, Map.class);

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new RuntimeException("Shiprocket API failed: " + response.getStatusCode());
    }

    Map<String, Object> responseBody = response.getBody();

    log.info("Shiprocket createShipment response: {}", responseBody);

    // ✅ CORRECT SUCCESS CHECK
    Object statusCode = responseBody.get("status_code");

    if (statusCode == null || Integer.parseInt(statusCode.toString()) != 1) {
        throw new RuntimeException("Shiprocket error: " + responseBody);
    }

    // ✅ CORRECT shipment_id extraction
    Object shipmentIdObj = responseBody.get("shipment_id");

    if (shipmentIdObj == null) {
        throw new RuntimeException("Shipment ID missing: " + responseBody);
    }

    // ❌ DO NOT EXPECT AWB HERE
    return ShipmentResponse.builder()
            .shipmentId(String.valueOf(shipmentIdObj))
            .trackingId(null)
            .status("CREATED")
            .courier("SHIPROCKET")
            .build();
}
    
    
    
    
    
    
    
    
    private Map<String, Object> buildRequest(Order order) {
       String phoneNumber = getValidPhoneNumber(order.getUser().getPhoneNumber());
    Map<String, Object> request = new HashMap<>();
    
    request.put("order_id", order.getId().toString());                            
    request.put("order_date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));  
    request.put("pickup_location", "Home");                                     
    request.put("comment", "Order from " + order.getUser().getName());            
    request.put("billing_customer_name", order.getUser().getName());               
    request.put("billing_last_name", "");                                          
    request.put("billing_address", order.getDeliveryAddressLine1());               
    request.put("billing_address_2", order.getDeliveryAddressLine2() != null ? 
                                     order.getDeliveryAddressLine2() : "");        
    request.put("billing_city", order.getDeliveryCity());                        
    request.put("billing_pincode", Integer.parseInt(order.getDeliveryPostalCode()));  
    request.put("billing_state", order.getDeliveryState());                        
    request.put("billing_country", order.getDeliveryCountry() != null ? 
                                   order.getDeliveryCountry() : "India");          
    request.put("billing_email", order.getUser().getEmail());                      
    request.put("billing_phone", phoneNumber); 
    request.put("billing_isd_code", "+91");                                     
    
    request.put("shipping_is_billing", true);                                      
    List<Map<String, Object>> orderItems = new ArrayList<>();
    for (OrderItem item : order.getItems()) {
        Map<String, Object> orderItem = new HashMap<>();
        orderItem.put("name", item.getProductName());                              
        orderItem.put("sku", item.getProductId().toString());                     
        orderItem.put("units", item.getQuantity());                               
        orderItem.put("selling_price", (int) Math.round(item.getPrice()));        
        orderItem.put("discount", 0);                                              
        orderItem.put("tax", 0);                                                   
        orderItem.put("hsn", 0);                                                   
        orderItems.add(orderItem);
    }
    request.put("order_items", orderItems);                                       
    request.put("payment_method", order.getPaymentMethod() == PaymentMethod.COD ? "COD" : "Prepaid");  
    request.put("shipping_charges", (int) Math.round(order.getShippingCharges()));  
    request.put("giftwrap_charges", 0);                                            
    request.put("transaction_charges", 0);                                         
    request.put("total_discount", order.getDiscountAmount() != null ? 
                                   (int) Math.round(order.getDiscountAmount()) : 0);  
    request.put("sub_total", (int) Math.round(order.getTotalAmount() - 
                                  order.getShippingCharges() - order.getTaxAmount()));  
    request.put("length", 10.0);                                                   
    request.put("breadth", 10.0);                                                  
    request.put("height", 10.0);                                                   
    request.put("weight", 0.5);                                                  
    
    return request;
}


@Override
public TrackingResponse trackShipment(String trackingId) {

    String url = BASE_URL + "/courier/track/awb/" + trackingId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getValidToken());

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new RuntimeException("Failed to fetch tracking from Shiprocket");
    }

    Map<String, Object> body = response.getBody();

    log.info("Shiprocket tracking response: {}", body);

    Map<String, Object> data = (Map<String, Object>) body.get("data");

    if (data == null) {
        throw new RuntimeException("Invalid tracking response: " + body);
    }

    List<Map<String, Object>> shipmentTrack =
            (List<Map<String, Object>>) data.get("shipment_track");

    if (shipmentTrack == null || shipmentTrack.isEmpty()) {
        return TrackingResponse.builder()
                .trackingId(trackingId)
                .events(List.of())
                .build();
    }

    List<Map<String, Object>> activities =
            (List<Map<String, Object>>) shipmentTrack.get(0).get("activities");

    List<TrackingResponse.Event> events = activities == null
            ? List.of()
            : activities.stream()
            .map(a -> TrackingResponse.Event.builder()
                    .status((String) a.get("activity"))
                    .date((String) a.get("date"))
                    .location((String) a.get("location"))
                    .build())
            .toList();

    return TrackingResponse.builder()
            .trackingId(trackingId)
            .events(events)
            .build();
}

    @Override
    public void cancelShipment(String shipmentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }private String getValidPhoneNumber(Integer phone) {
    if (phone == null) {
        return "9889808605"; 
    }
    String phoneStr = String.valueOf(phone);
    phoneStr = phoneStr.replaceAll("[^0-9]", "");
    if (phoneStr.length() >= 10) {
        return phoneStr.substring(phoneStr.length() - 10);
    }
    return String.format("%10s", phoneStr).replace(' ', '0');
}



@Transactional
public void handleWebhook(Map<String, Object> payload) {

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


@Override
public ShipmentResponse assignCourier(String shipmentId) {

    String url = BASE_URL + "/courier/assign/awb";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getValidToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = new HashMap<>();
    body.put("shipment_id", shipmentId);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response =
            restTemplate.postForEntity(url, entity, Map.class);

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new RuntimeException("Failed to assign courier");
    }

    Map<String, Object> responseBody = response.getBody();

    if (responseBody.containsKey("status") &&
            "error".equalsIgnoreCase(String.valueOf(responseBody.get("status")))) {

        throw new RuntimeException("Shiprocket error: " + responseBody.get("message"));
    }

    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

    if (data == null) {
        throw new RuntimeException("No data in assign courier response: " + responseBody);
    }

    String awb = String.valueOf(data.get("awb_code"));
    String courierName = String.valueOf(data.get("courier_name"));

    return ShipmentResponse.builder()
            .shipmentId(shipmentId)
            .trackingId(awb)            
            .courier(courierName)
            .status("AWB_ASSIGNED")
            .build();
}

    @Override
    public ServiceabilityResponse checkServiceability(
            String pickupPincode,
            String deliveryPincode,
            double weight,
            boolean cod) {

        String url = BASE_URL + "/courier/serviceability"
                + "?pickup_postcode=" + pickupPincode
                + "&delivery_postcode=" + deliveryPincode
                + "&weight=" + weight
                + "&cod=" + (cod ? 1 : 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Serviceability API failed");
        }

        Map<String, Object> body = response.getBody();
        log.info("Serviceability response: {}", body);

        Map<String, Object> data = (Map<String, Object>) body.get("data");

        if (data == null) {
            return ServiceabilityResponse.builder()
                    .serviceable(false)
                    .couriers(List.of())
                    .build();
        }

        List<Map<String, Object>> couriers =
                (List<Map<String, Object>>) data.get("available_courier_companies");

        if (couriers == null || couriers.isEmpty()) {
            return ServiceabilityResponse.builder()
                    .serviceable(false)
                    .couriers(List.of())
                    .build();
        }

      List<ServiceabilityResponse.CourierOption> options =
        couriers.stream()
        .map(c -> {

            String courierName = (String) c.get("courier_name");

            Double rate = c.get("rate") != null
                    ? Double.valueOf(c.get("rate").toString())
                    : 0.0;

            Integer deliveryDays = c.get("estimated_delivery_days") != null
                    ? Integer.valueOf(c.get("estimated_delivery_days").toString())
                    : 0;

            Boolean codAvailable = c.get("cod") != null
                    && Integer.valueOf(c.get("cod").toString()) == 1;

            return ServiceabilityResponse.CourierOption.builder()
                    .courierName(courierName)
                    .rate(rate)
                    .deliveryDays(deliveryDays)
                    .codAvailable(codAvailable)
                    .build();
        })
        .toList();

        return ServiceabilityResponse.builder()
                .serviceable(true)
                .couriers(options)
                .build();
    }


// @Override
// public Map<String, Object> trackShipment(String trackingId) {

//     String url = BASE_URL + "/courier/track/awb/" + trackingId;

//     HttpHeaders headers = new HttpHeaders();
//     headers.setBearerAuth(getValidToken());

//     HttpEntity<Void> entity = new HttpEntity<>(headers);

//     ResponseEntity<Map> response =
//             restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

//     Map<String, Object> body = response.getBody();

//     log.info("Tracking response: {}", body);

//     return body;
// }

}