package com.xyz.entertainment.ticketing.user.service;

/**
 * Thrown when partner login cannot resolve a tenant (e.g. no tenantId provided
 * and user has zero or multiple PARTNER tenants, or tenantId not found / user not a member).
 * Maps to 400 Bad Request.
 */
public class TenantRequiredException extends RuntimeException {

    public TenantRequiredException(String message) {
        super(message);
    }
}
