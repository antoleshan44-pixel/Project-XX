package com.urbano.monolith.auth.service;

import com.urbano.common.exception.SmsDeliveryException;
import com.urbano.monolith.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String OTP_REGISTER_PREFIX = "otp:register:";
    private static final String OTP_RESET_PREFIX = "otp:reset:";
    private static final Duration OTP_REGISTER_TTL = Duration.ofMinutes(5);
    private static final Duration OTP_RESET_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;
    private final SmsService smsService;

    /**
     * Generate a 6-digit OTP, store it in Redis, and send via SMS.
     * Throws SmsDeliveryException if SMS fails; in that case the OTP is discarded
     * so the caller can safely retry without leaving a live code orphaned in Redis.
     */
    public String generateAndSendPhoneOtp(String phone) {
        return generateAndSend(
                OTP_REGISTER_PREFIX + phone,
                OTP_REGISTER_TTL,
                phone,
                "Your Urbano Homes verification code is: "
        );
    }

    public String generateAndSendResetOtp(String userId, String phone) {
        return generateAndSend(
                OTP_RESET_PREFIX + userId,
                OTP_RESET_TTL,
                phone,
                "Your Urbano Homes password reset code is: "
        );
    }

    private String generateAndSend(String key, Duration ttl, String phone, String prefix) {
        String otp = generateOtp();
        String message = prefix + otp;

        redisTemplate.opsForValue().set(key, otp, ttl);

        try {
            smsService.sendSms(phone, message);
            log.info("OTP sent to {}", maskPhone(phone));
            return otp;
        } catch (SmsDeliveryException e) {
            redisTemplate.delete(key);
            log.error("OTP send failed for {} — Redis key removed", maskPhone(phone));
            throw e;
        }
    }

    public boolean verifyPhoneOtp(String phone, String code) {
        String key = OTP_REGISTER_PREFIX + phone;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public boolean verifyResetOtp(String userId, String code) {
        String key = OTP_RESET_PREFIX + userId;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}