package com.project.backend.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryConsume(String key, int limit, Duration duration) {
        try {
            String counterKey = "rate_limit:" + key;
            
            // Get current count
            String currentCountStr = redisTemplate.opsForValue().get(counterKey);
            long currentCount = currentCountStr == null ? 0 : Long.parseLong(currentCountStr);
            
            if (currentCount >= limit) {
                return false;
            }
            
            // Increment counter
            Long newCount = redisTemplate.opsForValue().increment(counterKey);
            
            // Set expiry on first increment
            if (newCount == 1) {
                redisTemplate.expire(counterKey, duration);
            }
            
            return newCount <= limit;
            
        } catch (Exception e) {
            log.error("Error in rate limiting for key: {}", key, e);
            return true; // Fallback: allow request
        }
    }
    
    public long getRemainingTokens(String key, int limit, Duration duration) {
        try {
            String counterKey = "rate_limit:" + key;
            String currentCountStr = redisTemplate.opsForValue().get(counterKey);
            long currentCount = currentCountStr == null ? 0 : Long.parseLong(currentCountStr);
            
            return Math.max(0, limit - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining tokens for key: {}", key, e);
            return limit;
        }
    }
}