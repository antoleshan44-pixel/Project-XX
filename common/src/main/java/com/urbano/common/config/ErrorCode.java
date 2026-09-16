package com.urbano.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth errors (AUTH-1000 to AUTH-1999)
    AUTH_1000("AUTH-1000", "Authentication required"),
    AUTH_1001("AUTH-1001", "Invalid credentials"),
    AUTH_1002("AUTH-1002", "Token expired"),
    AUTH_1003("AUTH-1003", "Invalid token"),
    AUTH_1004("AUTH-1004", "Account suspended"),
    AUTH_1005("AUTH-1005", "Email already registered"),
    AUTH_1006("AUTH-1006", "Phone already registered"),
    AUTH_1007("AUTH-1007", "Invalid refresh token"),
    AUTH_1008("AUTH-1008", "Invalid activation token"),
    AUTH_1009("AUTH-1009", "Activation token expired"),

    // User errors (USER-2000 to USER-2999)
    USER_2000("USER-2000", "User not found"),
    USER_2001("USER-2001", "User already exists"),
    USER_2002("USER-2002", "User is inactive"),

    // Property errors (PROP-3000 to PROP-3999)
    PROP_3000("PROP-3000", "Property not found"),
    PROP_3001("PROP-3001", "Unit not found"),
    PROP_3002("PROP-3002", "Unit already occupied"),
    PROP_3003("PROP-3003", "Unit cannot be published"),
    PROP_3004("PROP-3004", "Invalid unit status"),

    // Tenant errors (TEN-4000 to TEN-4999)
    TEN_4000("TEN-4000", "Tenant not found"),
    TEN_4001("TEN-4001", "Tenant already exists"),
    TEN_4002("TEN-4002", "Lease not found"),
    TEN_4003("TEN-4003", "Unit already has active lease"),

    // Payment errors (PAY-5000 to PAY-5999)
    PAY_5000("PAY-5000", "Payment not found"),
    PAY_5001("PAY-5001", "Payment already reconciled"),
    PAY_5002("PAY-5002", "Invalid payment amount"),
    PAY_5003("PAY-5003", "Invalid M-Pesa callback"),
    PAY_5004("PAY-5004", "Duplicate payment"),

    // Maintenance errors (MAINT-6000 to MAINT-6999)
    MAINT_6000("MAINT-6000", "Maintenance request not found"),
    MAINT_6001("MAINT-6001", "Invalid status transition"),

    // CRM errors (CRM-7000 to CRM-7999)
    CRM_7000("CRM-7000", "Contact not found"),
    CRM_7001("CRM-7001", "Contact already exists"),
    CRM_7002("CRM-7002", "Interaction not found"),

    // Validation errors (VAL-8000 to VAL-8999)
    VAL_8000("VAL-8000", "Validation failed"),

    // System errors (SYS-9000 to SYS-9999)
    SYS_9000("SYS-9000", "Internal server error"),
    SYS_9001("SYS-9001", "Service unavailable"),
    SYS_9002("SYS-9002", "Rate limit exceeded");

    private final String code;
    private final String message;

    public String getFullMessage(String detail) {
        return code + ": " + message + (detail != null ? " - " + detail : "");
    }
}