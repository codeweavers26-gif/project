package com.project.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.MessageResponse;
import com.project.backend.entity.NewsletterSubscriber;
import com.project.backend.service.NewsletterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@Tag(name = "Newsletter", description = "Inner Circle subscription")
public class NewsletterController {

    private final NewsletterService newsletterService;

    @Operation(summary = "Subscribe to the newsletter")
    @PostMapping("/subscribe")
    public ResponseEntity<MessageResponse> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email is required"));
        }
        String msg = newsletterService.subscribe(email);
        return ResponseEntity.ok(new MessageResponse(msg));
    }

    @Operation(summary = "Admin — get all subscribers")
    @GetMapping("/subscribers")
    public ResponseEntity<List<NewsletterSubscriber>> getAll() {
        return ResponseEntity.ok(newsletterService.getAll());
    }
}
