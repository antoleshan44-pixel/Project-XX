package com.urbano.monolith.payment.service;

import com.urbano.monolith.payment.repository.PaymentRepository;  // ✅ Add this import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentReminderService {

    private final PaymentRepository paymentRepository;

    public void sendReminders() {
        log.info("Sending rent reminders...");
        // TODO: Implement rent reminder logic
        // 1. Query active leases
        // 2. Check which are due in 3 days
        // 3. Send SMS reminders via Africa's Talking
    }
}