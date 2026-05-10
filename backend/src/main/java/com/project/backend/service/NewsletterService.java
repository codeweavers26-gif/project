package com.project.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.NewsletterSubscriber;
import com.project.backend.repository.NewsletterSubscriberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;

    @Transactional
    public String subscribe(String email) {
        String normalised = email.trim().toLowerCase();

        if (repository.existsByEmail(normalised)) {
            // Already subscribed — re-activate if inactive
            NewsletterSubscriber existing = repository.findByEmail(normalised).get();
            if (!existing.isActive()) {
                existing.setActive(true);
                repository.save(existing);
                return "Welcome back! You have been re-subscribed.";
            }
            return "You are already subscribed.";
        }

        NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                .email(normalised)
                .active(true)
                .build();

        repository.save(subscriber);
        log.info("New newsletter subscriber: {}", normalised);
        return "Subscribed successfully!";
    }
}
