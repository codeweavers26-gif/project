package com.project.backend.service;

import com.project.backend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponScheduler {

    private final CouponRepository couponRepository;

    // All coupon dates are stored in IST (user's timezone).
    // Render server runs in UTC, so LocalDateTime.now() would be UTC.
    // We must use IST for correct comparison.
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /**
     * Runs every minute.
     * Activates SCHEDULED coupons whose validFrom <= now (IST).
     * Expires ACTIVE coupons whose validTo < now (IST).
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void syncCouponStatuses() {
        LocalDateTime now = LocalDateTime.now(IST);

        int activated = couponRepository.activateScheduledCoupons(now);
        int expired   = couponRepository.expireCoupons(now);

        if (activated > 0 || expired > 0) {
            log.info("Coupon scheduler: activated={}, expired={}", activated, expired);
        }
    }
}
