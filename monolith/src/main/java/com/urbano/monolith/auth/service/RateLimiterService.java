package com.urbano.monolith.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributed rate limiter with Redis backing and in-memory fallback.
 * Protects auth and sensitive public endpoints from brute-force and credential stuffing.
 */
@Slf4j
@Service
public class RateLimiterService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // In-memory fallback if Redis is unavailable
    private final ConcurrentHashMap<String, InMemoryBucket> memoryBuckets = new ConcurrentHashMap<>();

    private static class InMemoryBucket {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long resetTimeEpochSeconds;

        InMemoryBucket(int windowSeconds) {
            this.resetTimeEpochSeconds = Instant.now().getEpochSecond() + windowSeconds;
        }
    }

    /**
     * Checks if the given action for an identifier (e.g., client IP) is permitted.
     *
     * @param action        action identifier (e.g. "auth_login")
     * @param identifier    client identifier (e.g. IP address)
     * @param maxRequests   maximum allowed requests within window
     * @param windowSeconds time window in seconds
     * @return true if allowed, false if limit exceeded
     */
    public boolean tryAcquire(String action, String identifier, int maxRequests, int windowSeconds) {
        String key = "ratelimit:" + action + ":" + identifier;

        if (redisTemplate != null) {
            try {
                Long current = redisTemplate.opsForValue().increment(key);
                if (current != null && current == 1L) {
                    redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
                }
                if (current != null && current > maxRequests) {
                    log.warn("Rate limit exceeded for key '{}' ({} > {})", key, current, maxRequests);
                    return false;
                }
                return true;
            } catch (Exception ex) {
                log.debug("Redis rate limit check failed, falling back to in-memory: {}", ex.getMessage());
            }
        }

        // In-memory fallback
        long now = Instant.now().getEpochSecond();
        InMemoryBucket bucket = memoryBuckets.compute(key, (k, existing) -> {
            if (existing == null || now >= existing.resetTimeEpochSeconds) {
                InMemoryBucket newBucket = new InMemoryBucket(windowSeconds);
                newBucket.count.set(1);
                return newBucket;
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        if (bucket.count.get() > maxRequests) {
            log.warn("In-memory rate limit exceeded for key '{}' ({} > {})", key, bucket.count.get(), maxRequests);
            return false;
        }
        return true;
    }

    /**
     * Resets / clears the rate limit for a specific action and identifier (e.g. on successful login).
     */
    public void reset(String action, String identifier) {
        String key = "ratelimit:" + action + ":" + identifier;
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
            }
        } catch (Exception ignored) {
        }
        memoryBuckets.remove(key);
    }
}
