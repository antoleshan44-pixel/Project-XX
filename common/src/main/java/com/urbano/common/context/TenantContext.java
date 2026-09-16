package com.urbano.common.context;

import com.urbano.common.enums.UserRole;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> CURRENT_USER_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_PM_ACCOUNT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_ADMIN_BYPASS = ThreadLocal.withInitial(() -> false);

    public static void setUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setUserRole(UserRole role) {
        CURRENT_USER_ROLE.set(role);
    }

    public static UserRole getUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    public static void setPmAccountId(UUID pmAccountId) {
        CURRENT_PM_ACCOUNT_ID.set(pmAccountId);
    }

    public static UUID getPmAccountId() {
        return CURRENT_PM_ACCOUNT_ID.get();
    }

    public static void setAdminBypass(boolean bypass) {
        IS_ADMIN_BYPASS.set(bypass);
    }

    public static boolean isAdminBypass() {
        return IS_ADMIN_BYPASS.get() != null && IS_ADMIN_BYPASS.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
        CURRENT_PM_ACCOUNT_ID.remove();
        IS_ADMIN_BYPASS.remove();
    }
}