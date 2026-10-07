package com.urbano.monolith.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived 6-digit codes for tenant invites.
 *
 * <p>Only the code lives in Redis. The authoritative invite state is on the
 * {@code tenants} row (invite_status, invited_at). If Redis is flushed, the PM
 * just calls resend-invite and a new code is generated.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteTokenService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String INVITE_CODE_PREFIX = "invite:code:";
    private static final Duration INVITE_CODE_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    /** Key is scoped to (tenantId) so resend overwrites cleanly. */
    public String generateCodeForTenant(String tenantId) {
        String code = generateOtp();
        redisTemplate.opsForValue().set(INVITE_CODE_PREFIX + tenantId, code, INVITE_CODE_TTL);
        log.debug("Invite code generated for tenant {} (ttl={}s): CODE={}", tenantId, INVITE_CODE_TTL.toSeconds(), code);
        return code;
    }

    /**
     * Verifies the code for the given tenant, deleting it on success so it
     * can't be replayed.
     */
    public boolean verifyAndConsume(String tenantId, String code) {
        String key = INVITE_CODE_PREFIX + tenantId;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public void invalidate(String tenantId) {
        redisTemplate.delete(INVITE_CODE_PREFIX + tenantId);
    }

    public Duration getCodeTtl() {
        return INVITE_CODE_TTL;
    }

    // ============================================================
    // Phase 3 (admin): read-only inspection for SUPER_ADMIN support tooling
    // ============================================================

    /**
     * Returns the current invite code for the tenant WITHOUT consuming it.
     * Returns null if no code exists (never generated, already consumed, or
     * expired).
     */
    public String peek(String tenantId) {
        return redisTemplate.opsForValue().get(INVITE_CODE_PREFIX + tenantId);
    }

    /**
     * Remaining TTL of the invite code in seconds.
     * Redis semantics: -2 = key does not exist, -1 = key exists with no expiry.
     * Both map to null here so callers can treat "no live code" as one case.
     */
    public Long ttlSeconds(String tenantId) {
        Long ttl = redisTemplate.getExpire(INVITE_CODE_PREFIX + tenantId, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return null;
        }
        return ttl;
    }

    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}