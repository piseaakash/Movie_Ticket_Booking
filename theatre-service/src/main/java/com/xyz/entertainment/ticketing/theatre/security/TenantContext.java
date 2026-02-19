package com.xyz.entertainment.ticketing.theatre.security;

import java.util.Collections;
import java.util.List;

/**
 * Simple thread-local context holding userId, tenantId and roles extracted
 * from the JWT. Used by theatre-service to scope access by tenant and role.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    private final Long userId;
    private final Long tenantId;
    private final List<String> roles;

    private TenantContext(Long userId, Long tenantId, List<String> roles) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles != null ? List.copyOf(roles) : List.of();
    }

    public static void set(Long userId, Long tenantId, List<String> roles) {
        HOLDER.set(new TenantContext(userId, tenantId, roles));
    }

    public static TenantContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public boolean hasAnyRole(String... required) {
        if (roles == null || roles.isEmpty() || required == null || required.length == 0) {
            return false;
        }
        for (String r : required) {
            if (roles.contains(r)) {
                return true;
            }
        }
        return false;
    }
}

