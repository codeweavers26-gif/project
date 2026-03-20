package com.project.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.ResponseDto.TrackingResponse;
import com.project.backend.config.ShippingProvider;
import com.project.backend.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketService implements ShippingProvider {

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

        // 🔥 SAVE TOKEN
        this.token = newToken;

        // 🔥 SET EXPIRY (safe buffer)
        this.tokenExpiry = LocalDateTime.now().plusHours(9);

        log.info("Shiprocket token refreshed");

    } catch (Exception e) {
        log.error("Shiprocket authentication failed", e);
        throw new RuntimeException("Failed to authenticate with Shiprocket", e);
    }
}

private String getValidToken() {

    if (token == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
        log.info("Token expired or missing, re-authenticating...");
        authenticate();
    }

    return token;
}
    // private void authenticate() {
    //     String url = BASE_URL + "/auth/login";

    //     Map<String, String> request = Map.of(
    //             "email", "yadavpiyush8302@gmail.com",
    //             "password", "VWzIzOV14xyGhkWSYbHo&LF!bXqaN^D8"
    //     );

    //     ResponseEntity<Map> response =
    //             restTemplate.postForEntity(url, request, Map.class);

    //     this.token = (String) ((Map) response.getBody().get("data")).get("token");
    // }

    @Override
    public ShipmentResponse createShipment(Order order) {

        if (token == null) {
            authenticate();
        }

        String url = BASE_URL + "/orders/create/adhoc";

        HttpHeaders headers = new HttpHeaders();
          headers.setBearerAuth(getValidToken()); 
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = buildRequest(order);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, entity, Map.class);

        Map data = (Map) response.getBody().get("data");

        return ShipmentResponse.builder()
                .shipmentId(String.valueOf(data.get("shipment_id")))
                .trackingId((String) data.get("awb_code"))
                .status("CREATED")
                .courier("SHIPROCKET")
                .build();
    }
private Map<String, Object> buildRequest(Order order) {

    Map<String, Object> request = new HashMap<>();

    request.put("order_id", order.getId());
    request.put("order_date", LocalDate.now().toString());
    request.put("billing_customer_name", order.getUser().getName());
    request.put("billing_address", order.getDeliveryAddressLine1());
    request.put("billing_city", order.getDeliveryCity());
    request.put("billing_pincode", order.getDeliveryPostalCode());
    request.put("billing_state", order.getDeliveryState());
    request.put("billing_country", "India");
    request.put("billing_email", order.getUser().getEmail());
    request.put("billing_phone", "9999999999");

    request.put("order_items", List.of(
            Map.of(
                    "name", "Product",
                    "units", 1,
                    "selling_price", order.getTotalAmount()
            )
    ));

    request.put("payment_method", "Prepaid");
    request.put("sub_total", order.getTotalAmount());
    request.put("length", 10);
    request.put("breadth", 10);
    request.put("height", 10);
    request.put("weight", 0.5);

    return request;
}

   @Override
public TrackingResponse trackShipment(String trackingId) {

    if (token == null) {
        authenticate();
    }

    String url = BASE_URL + "/courier/track/awb/" + trackingId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getValidToken()); 

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

    Map body = response.getBody();

    if (body == null || body.get("data") == null) {
        throw new RuntimeException("Invalid tracking response");
    }

    Map data = (Map) body.get("data");

    List<Map> tracks = (List<Map>) data.get("shipment_track");

    if (tracks == null || tracks.isEmpty()) {
        throw new RuntimeException("No tracking data found");
    }

    Map latest = tracks.get(0);

    return TrackingResponse.builder()
            .status((String) latest.get("current_status"))
            .location((String) latest.get("current_location"))
            .estimatedDeliveryDate(
                    latest.get("edd") != null ? latest.get("edd").toString() : null
            )
            .build();
}

    @Override
    public void cancelShipment(String shipmentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}