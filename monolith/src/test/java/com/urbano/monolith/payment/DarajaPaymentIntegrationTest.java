package com.urbano.monolith.payment;

import com.urbano.common.enums.PaymentStatus;
import com.urbano.common.enums.PropertyStatus;
import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.enums.UnitStatus;
import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.payment.dto.DarajaCallbackRequest;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.property.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class DarajaPaymentIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UnitRepository unitRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        unitRepository.deleteAll();
        propertyRepository.deleteAll();
    }

    @Test
    void testProcessDarajaCallbackReconciledUnitMatch() throws Exception {
        Property property = Property.builder()
                .pmAccountId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .name("Kilimani Plaza")
                .address("Lenana Road")
                .city("Nairobi")
                .type("RESIDENTIAL")
                .totalUnits(10)
                .status(PropertyStatus.AVAILABLE)
                .build();
        Property savedProp = propertyRepository.save(property);

        Unit unit = Unit.builder()
                .property(savedProp)
                .unitNumber("A101")
                .floor(1)
                .bedrooms(2)
                .bathrooms(1)
                .rentAmount(45000.0)
                .currency("KES")
                .squareFootage(85.5)
                .isAvailable(true)
                .status(UnitStatus.AVAILABLE)
                .propertyType(PropertyType.APARTMENT)
                .transactionType(TransactionType.FOR_RENT)
                .published(true)
                .build();
        Unit savedUnit = unitRepository.save(unit);

        DarajaCallbackRequest callback = DarajaCallbackRequest.builder()
                .transactionType("Pay Bill")
                .transactionId("QWX98765432")
                .transactionTime("20260917103000")
                .amount(new BigDecimal("45000.00"))
                .businessShortCode("600000")
                .billRefNumber("UNIT-A101")
                .phoneNumber("254712345678")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockMvc.perform(post("/api/callbacks/daraja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Callback processed successfully")));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments, hasSize(1));
        Payment payment = payments.get(0);
        assertThat(payment.getMpesaReceiptNumber(), is("QWX98765432"));
        assertThat(payment.getAmount(), is(new BigDecimal("45000.00")));
        assertThat(payment.getUnitId(), is(savedUnit.getId()));
        assertThat(payment.getStatus(), is(PaymentStatus.RECONCILED));
        assertThat(payment.isReconciled(), is(true));
    }

    @Test
    void testProcessDarajaCallbackPartialPayment() throws Exception {
        Property property = Property.builder()
                .pmAccountId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .name("Kilimani Plaza")
                .address("Lenana Road")
                .city("Nairobi")
                .type("RESIDENTIAL")
                .totalUnits(10)
                .status(PropertyStatus.AVAILABLE)
                .build();
        Property savedProp = propertyRepository.save(property);

        Unit unit = Unit.builder()
                .property(savedProp)
                .unitNumber("B202")
                .floor(2)
                .bedrooms(1)
                .bathrooms(1)
                .rentAmount(30000.0)
                .currency("KES")
                .squareFootage(50.0)
                .isAvailable(true)
                .status(UnitStatus.AVAILABLE)
                .propertyType(PropertyType.APARTMENT)
                .transactionType(TransactionType.FOR_RENT)
                .published(true)
                .build();
        Unit savedUnit = unitRepository.save(unit);

        DarajaCallbackRequest callback = DarajaCallbackRequest.builder()
                .transactionType("Pay Bill")
                .transactionId("QWX55566677")
                .transactionTime("20260917103000")
                .amount(new BigDecimal("15000.00"))
                .businessShortCode("600000")
                .billRefNumber("B202")
                .phoneNumber("254712345678")
                .firstName("Alice")
                .lastName("Smith")
                .build();

        mockMvc.perform(post("/api/callbacks/daraja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk());

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments, hasSize(1));
        Payment payment = payments.get(0);
        assertThat(payment.getUnitId(), is(savedUnit.getId()));
        assertThat(payment.getStatus(), is(PaymentStatus.PARTIAL));
    }

    @Test
    void testProcessDarajaCallbackUnmatchedUnit() throws Exception {
        DarajaCallbackRequest callback = DarajaCallbackRequest.builder()
                .transactionType("Pay Bill")
                .transactionId("QWX99999999")
                .transactionTime("20260917103000")
                .amount(new BigDecimal("20000.00"))
                .businessShortCode("600000")
                .billRefNumber("UNKNOWN-REF")
                .phoneNumber("254712345678")
                .firstName("Bob")
                .lastName("Jones")
                .build();

        mockMvc.perform(post("/api/callbacks/daraja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UNMATCHED")));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments, hasSize(1));
        assertThat(payments.get(0).getStatus(), is(PaymentStatus.UNMATCHED));
    }

    @Test
    void testProcessDuplicateDarajaCallbackIdempotent() throws Exception {
        DarajaCallbackRequest callback = DarajaCallbackRequest.builder()
                .transactionType("Pay Bill")
                .transactionId("QWX11122233")
                .transactionTime("20260917103000")
                .amount(new BigDecimal("25000.00"))
                .businessShortCode("600000")
                .billRefNumber("UNIT-B202")
                .phoneNumber("254788990011")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        // First callback
        mockMvc.perform(post("/api/callbacks/daraja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk());

        // Duplicate callback with same transactionId
        mockMvc.perform(post("/api/callbacks/daraja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Duplicate ignored")));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments, hasSize(1));
    }

    @Test
    void testProcessDarajaCallbackInvalidPayload() throws Exception {
        DarajaCallbackRequest invalidCallback = DarajaCallbackRequest.builder()
                .transactionId("")
                .amount(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/api/callbacks/daraja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCallback)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid payload")));
    }
}
