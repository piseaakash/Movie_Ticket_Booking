package com.xyz.entertainment.ticketing.user.client;

import java.util.List;

/**
 * Client for auth-service token issuance. Used after successful login
 * to obtain access and refresh tokens without user-service holding JWT secrets.
 */
public interface AuthClient {

    /**
     * Request access and refresh tokens for an already-authenticated user.
     *
     * @param userId   user id (e.g. from user_accounts.id)
     * @param email    user email
     * @param roles    logical roles, e.g. CUSTOMER for B2C or PARTNER_ADMIN for B2B
     * @param tenantId optional; when present, auth-service adds tenantId claim to JWT for tenant-scoped access
     * @return token response from auth-service
     * @throws AuthServiceUnavailableException if auth-service is unreachable or returns an error
     */
    AuthTokenResponse issueTokens(Long userId, String email, List<String> roles, Long tenantId);
}
