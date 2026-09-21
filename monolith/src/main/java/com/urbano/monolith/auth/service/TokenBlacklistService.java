package com.urbano.monolith.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_EPOCH_PREFIX = "user:epoch:";

    // Epoch entries need to outlive the longest-lived token.
    // refresh-expiry default is 7 days; we use 8 to be safe.
    private static final Duration EPOCH_TTL = Duration.ofDays(8);

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    // ============================================================
    // SINGLE-TOKEN BLACKLIST (logout)
    // ============================================================
    public void blacklistToken(String token) {
        try {
            String jti = jwtService.extractTokenId(token);
            if (jti == null) {
                log.warn("Cannot blacklist token: no jti claim present");
                return;
            }

            long ttlMillis = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                // Already expired — nothing to blacklist
                return;
            }

            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "true",
                    ttlMillis,
                    TimeUnit.MILLISECONDS
            );
            log.info("Token blacklisted: jti={}, ttl={}ms", jti, ttlMillis);
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String jti = jwtService.extractTokenId(token);
            if (jti == null) return false;
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            // Fail closed: if we can't determine, treat as blacklisted
            log.warn("Blacklist lookup failed, failing closed: {}", e.getMessage());
            return true;
        }
    }

    // ============================================================
    // USER-EPOCH INVALIDATION (password reset, "logout everywhere")
    // ============================================================
    // Any token issued BEFORE the epoch is rejected, regardless of jti.
    // This is how we invalidate refresh tokens we never stored.
    public void invalidateAllForUser(UUID userId) {
        if (userId == null) return;
        redisTemplate.opsForValue().set(
                USER_EPOCH_PREFIX + userId,
                String.valueOf(System.currentTimeMillis()),
                EPOCH_TTL
        );
        log.info("All tokens invalidated for user {}", userId);
    }

    /**
     * @return true if the token was issued at/after the user's epoch (still valid),
     *         false if it predates the epoch (must be rejected).
     */
    public boolean isWithinUserEpoch(String token) {
        try {
            UUID userId = jwtService.extractUserId(token);
            if (userId == null) return true;   // no userId claim, nothing to compare — let other checks decide

            String epochStr = redisTemplate.opsForValue().get(USER_EPOCH_PREFIX + userId);
            if (epochStr == null) return true;  // no epoch set, everything is fine

            long epochMillis = Long.parseLong(epochStr);
            Date issuedAt = jwtService.extractIssuedAt(token);
            if (issuedAt == null) return true;  // no iat, nothing to compare

            return issuedAt.getTime() >= epochMillis;
        } catch (Exception e) {
            log.warn("Epoch lookup failed, failing closed: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // Combined convenience used by both the filter and AuthService
    // ============================================================
    public boolean isTokenRevoked(String token) {
        return isTokenBlacklisted(token) || !isWithinUserEpoch(token);
    }
}