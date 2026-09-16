package com.urbano.monolith.auth.service;

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
     * Generate a 6-digit OTP and send via SMS
     */
    public String generateAndSendPhoneOtp(String phone) {
        String otp = generateOtp();
        String key = OTP_REGISTER_PREFIX + phone;
        redisTemplate.opsForValue().set(key, otp, OTP_REGISTER_TTL);

        String message = "Your Urbano Homes verification code is: " + otp;
        try {
            smsService.sendSms(phone, message);
            log.info("OTP sent to {}", phone);
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {}: {}", phone, e.getMessage());
        }
        return otp;
    }

    /**
     * Generate a 6-digit OTP for password reset
     */
    public String generateAndSendResetOtp(String userId, String phone) {
        String otp = generateOtp();
        String key = OTP_RESET_PREFIX + userId;
        redisTemplate.opsForValue().set(key, otp, OTP_RESET_TTL);

        String message = "Your Urbano Homes password reset code is: " + otp;
        try {
            smsService.sendSms(phone, message);
            log.info("Reset OTP sent to {}", phone);
        } catch (Exception e) {
            log.error("Failed to send reset OTP SMS to {}: {}", phone, e.getMessage());
        }
        return otp;
    }

    /**
     * Verify OTP for phone verification
     */
    public boolean verifyPhoneOtp(String phone, String code) {
        String key = OTP_REGISTER_PREFIX + phone;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * Verify OTP for password reset
     */
    public boolean verifyResetOtp(String userId, String code) {
        String key = OTP_RESET_PREFIX + userId;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * Generate a secure 6-digit OTP
     */
    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}