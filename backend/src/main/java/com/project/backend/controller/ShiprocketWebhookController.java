package com.project.backend.controller;

import com.project.backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/shiprocket")
@RequiredArgsConstructor
@Slf4j
public class ShiprocketWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/order")
    public ResponseEntity<String> handleOrderWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received order webhook from Shiprocket");
        webhookService.processOrderWebhook(payload);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/return")
    public ResponseEntity<String> handleReturnWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received return webhook from Shiprocket");
        webhookService.processReturnWebhook(payload);
        return ResponseEntity.ok("OK");
    }

    // @PostMapping("/payment")
    // public ResponseEntity<String> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
    //     log.info("Received payment webhook from Shiprocket");
    //     webhookService.processPaymentWebhook(payload);
    //     return ResponseEntity.ok("OK");
    // }
}