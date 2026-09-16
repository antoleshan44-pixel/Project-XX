package com.urbano.monolith.audit.constants;

/**
 * Standard audit event types used across all services
 */
public final class AuditEventType {

    private AuditEventType() {}

    // ============================================================
    // AUTH EVENTS
    // ============================================================
    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_REGISTER = "AUTH_REGISTER";
    public static final String AUTH_REFRESH = "AUTH_REFRESH";
    public static final String AUTH_PASSWORD_RESET = "AUTH_PASSWORD_RESET";
    public static final String AUTH_PHONE_VERIFY = "AUTH_PHONE_VERIFY";
    public static final String AUTH_PHONE_CONFIRM = "AUTH_PHONE_CONFIRM";

    // ============================================================
    // USER EVENTS
    // ============================================================
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String USER_SUSPENDED = "USER_SUSPENDED";
    public static final String USER_ACTIVATED = "USER_ACTIVATED";

    // ============================================================
    // PROPERTY EVENTS
    // ============================================================
    public static final String PROPERTY_CREATED = "PROPERTY_CREATED";
    public static final String PROPERTY_UPDATED = "PROPERTY_UPDATED";
    public static final String PROPERTY_DELETED = "PROPERTY_DELETED";
    public static final String PROPERTY_STATUS_CHANGED = "PROPERTY_STATUS_CHANGED";

    // ============================================================
    // UNIT EVENTS
    // ============================================================
    public static final String UNIT_CREATED = "UNIT_CREATED";
    public static final String UNIT_UPDATED = "UNIT_UPDATED";
    public static final String UNIT_DELETED = "UNIT_DELETED";
    public static final String UNIT_PUBLISHED = "UNIT_PUBLISHED";
    public static final String UNIT_UNPUBLISHED = "UNIT_UNPUBLISHED";
    public static final String UNIT_STATUS_CHANGED = "UNIT_STATUS_CHANGED";
    public static final String UNIT_OCCUPIED = "UNIT_OCCUPIED";
    public static final String UNIT_VACATED = "UNIT_VACATED";

    // ============================================================
    // TENANT EVENTS
    // ============================================================
    public static final String TENANT_CREATED = "TENANT_CREATED";
    public static final String TENANT_UPDATED = "TENANT_UPDATED";
    public static final String TENANT_DELETED = "TENANT_DELETED";
    public static final String TENANT_INVITED = "TENANT_INVITED";
    public static final String TENANT_ACTIVATED = "TENANT_ACTIVATED";

    // ============================================================
    // LEASE EVENTS
    // ============================================================
    public static final String LEASE_CREATED = "LEASE_CREATED";
    public static final String LEASE_UPDATED = "LEASE_UPDATED";
    public static final String LEASE_TERMINATED = "LEASE_TERMINATED";
    public static final String LEASE_RENEWED = "LEASE_RENEWED";

    // ============================================================
    // PAYMENT EVENTS
    // ============================================================
    public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
    public static final String PAYMENT_RECONCILED = "PAYMENT_RECONCILED";
    public static final String PAYMENT_UNRECONCILED = "PAYMENT_UNRECONCILED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_PROOF_SUBMITTED = "PAYMENT_PROOF_SUBMITTED";
    public static final String PAYMENT_PROOF_RESOLVED = "PAYMENT_PROOF_RESOLVED";
    public static final String MANAGEMENT_FEE_GENERATED = "MANAGEMENT_FEE_GENERATED";

    // ============================================================
    // MAINTENANCE EVENTS
    // ============================================================
    public static final String MAINTENANCE_REQUESTED = "MAINTENANCE_REQUESTED";
    public static final String MAINTENANCE_UPDATED = "MAINTENANCE_UPDATED";
    public static final String MAINTENANCE_RESOLVED = "MAINTENANCE_RESOLVED";

    // ============================================================
    // VIEWING EVENTS
    // ============================================================
    public static final String VIEWING_SCHEDULED = "VIEWING_SCHEDULED";
    public static final String VIEWING_UPDATED = "VIEWING_UPDATED";
    public static final String VIEWING_CANCELLED = "VIEWING_CANCELLED";
    public static final String VIEWING_COMPLETED = "VIEWING_COMPLETED";

    // ============================================================
    // SYSTEM EVENTS
    // ============================================================
    public static final String SYSTEM_STARTUP = "SYSTEM_STARTUP";
    public static final String SYSTEM_SHUTDOWN = "SYSTEM_SHUTDOWN";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String SCHEDULED_JOB_RUN = "SCHEDULED_JOB_RUN";
}