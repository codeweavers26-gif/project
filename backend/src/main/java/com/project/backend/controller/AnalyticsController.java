package com.project.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @PostMapping("/event")
    public ResponseEntity<Map<String, String>> trackEvent(
            @RequestBody(required = false) Map<String, Object> payload) {

        if (payload != null) {
            String event = payload.get("event") != null ? payload.get("event").toString() : "unknown";
            log.info("Analytics event received: {}", event);
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
