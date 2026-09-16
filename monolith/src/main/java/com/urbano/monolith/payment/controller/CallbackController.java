package com.urbano.monolith.payment.controller;

import com.urbano.monolith.payment.dto.DarajaCallbackRequest;
import com.urbano.monolith.payment.service.DarajaService;
import com.urbano.monolith.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/callbacks")
@RequiredArgsConstructor
public class CallbackController {

    private final PaymentService paymentService;
    private final DarajaService darajaService;

    /**
     * Process Daraja M-Pesa callback
     * ✅ PATCH 7: Added IP validation, idempotency, and full reconciliation
     */
    @PostMapping("/daraja")
    public ResponseEntity<String> processDarajaCallback(
            @RequestBody DarajaCallbackRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received Daraja callback: transactionId={}, billRefNumber={}, amount={}",
                request.getTransactionId(), request.getBillRefNumber(), request.getAmount());

        try {
            // ✅ 1. Validate source IP
            String clientIp = httpRequest.getRemoteAddr();
            if (!darajaService.isValidDarajaIp(clientIp)) {
                log.warn("Invalid Daraja callback IP: {}", clientIp);
                return ResponseEntity.status(403).body("Forbidden");
            }

            // ✅ 2. Validate callback payload
            if (!darajaService.validateCallback(request)) {
                log.warn("Invalid Daraja callback payload: {}", request);
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            // ✅ 3. Process callback with idempotency
            String result = darajaService.processCallback(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing Daraja callback: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error processing callback: " + e.getMessage());
        }
    }
}