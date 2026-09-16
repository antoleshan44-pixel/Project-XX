package com.urbano.monolith.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    public void blacklistToken(String token) {
        try {
            String jti = jwtService.extractTokenId(token);
            if (jti != null) {
                long ttl = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(
                            BLACKLIST_PREFIX + jti,
                            "true",
                            ttl,
                            TimeUnit.MILLISECONDS
                    );
                    log.info("Token blacklisted: {}", jti);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public void blacklistAllUserTokens(UUID userId) {
        String key = USER_TOKENS_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("All tokens invalidated for user: {}", userId);
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String jti = jwtService.extractTokenId(token);
            if (jti == null) return false;
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isTokenBlacklisted(String token, String email) {
        try {
            String jti = jwtService.extractTokenId(token);
            if (jti == null) return false;

            Boolean isBlacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
            if (isBlacklisted != null && isBlacklisted) {
                return true;
            }

            // Check if user has been logged out
            String userKey = USER_TOKENS_PREFIX + email;
            return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        } catch (Exception e) {
            return true;
        }
    }
}