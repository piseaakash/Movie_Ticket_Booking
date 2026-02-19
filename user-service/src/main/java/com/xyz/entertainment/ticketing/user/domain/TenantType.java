package com.xyz.entertainment.ticketing.user.domain;

/**
 * Type of tenant in the platform. PLATFORM is the single B2C tenant;
 * PARTNER represents a business (e.g. cinema chain or single theatre) onboarded for B2B.
 */
public enum TenantType {
    PLATFORM,
    PARTNER
}
