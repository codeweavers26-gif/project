package com.project.backend.service;

import com.project.backend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponScheduler {

    private final CouponRepository couponRepository;

    /**
     * Runs every minute.
     * Activates SCHEDULED coupons whose validFrom <= now.
     * Expires ACTIVE coupons whose validTo < now.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void syncCouponStatuses() {
        LocalDateTime now = LocalDateTime.now();

        int activated = couponRepository.activateScheduledCoupons(now);
        int expired   = couponRepository.expireCoupons(now);

        if (activated > 0 || expired > 0) {
            log.info("Coupon scheduler: activated={}, expired={}", activated, expired);
        }
    }
}
