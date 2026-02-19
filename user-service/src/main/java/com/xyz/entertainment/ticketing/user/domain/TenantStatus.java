package com.xyz.entertainment.ticketing.user.domain;

/**
 * Lifecycle status of a tenant. ACTIVE allows login and API access;
 * PENDING_APPROVAL can be used when partner registration requires platform approval.
 */
public enum TenantStatus {
    ACTIVE,
    PENDING_APPROVAL
}
