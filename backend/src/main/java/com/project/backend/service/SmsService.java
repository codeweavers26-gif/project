package com.project.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    @Async
    public void sendOtp(String phone, String otp) {

        if (accountSid.isEmpty() || authToken.isEmpty() || fromNumber.isEmpty()) {
            log.warn("Twilio not configured, skipping SMS OTP to {}", phone);
            return;
        }

        try {
            String toNumber = phone.startsWith("+") ? phone : "+91" + phone;

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            // Basic Auth header
            String credentials = accountSid + ":" + authToken;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encoded);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", fromNumber);
            body.add("To", toNumber);
            body.add("Body", "Your Rich & Retired OTP is: " + otp + ". Valid for 5 minutes. Do not share.");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP SMS sent to {} via Twilio", toNumber);
            } else {
                log.error("Twilio SMS failed: status={}, body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Twilio SMS exception for {}: {}", phone, e.getMessage(), e);
        }
    }
}
