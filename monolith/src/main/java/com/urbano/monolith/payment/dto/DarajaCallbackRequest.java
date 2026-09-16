package com.urbano.monolith.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DarajaCallbackRequest {

    // M-Pesa Standard Fields
    @JsonProperty("TransactionType")
    private String transactionType;

    @JsonProperty("TransID")
    private String transactionId;

    @JsonProperty("TransTime")
    private String transactionTime;

    @JsonProperty("TransAmount")
    private BigDecimal amount;

    @JsonProperty("BusinessShortCode")
    private String businessShortCode;

    @JsonProperty("BillRefNumber")
    private String billRefNumber;  // Unit/house number

    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;

    @JsonProperty("OrgAccountBalance")
    private String orgAccountBalance;

    @JsonProperty("ThirdPartyTransID")
    private String thirdPartyTransId;

    @JsonProperty("MSISDN")
    private String phoneNumber;  // Customer phone

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("MiddleName")
    private String middleName;

    @JsonProperty("LastName")
    private String lastName;

    // Additional fields
    private String mpesaReceiptNumber;
    private LocalDateTime transactionDate;
    private String customerName;
    private String rawPayload;

    // Helper to get full customer name
    public String getCustomerName() {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}