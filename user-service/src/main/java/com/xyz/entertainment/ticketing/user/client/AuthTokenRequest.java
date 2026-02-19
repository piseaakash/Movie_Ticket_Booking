package com.xyz.entertainment.ticketing.user.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Request body for auth-service POST /api/auth/tokens.
 * Kept in user-service to avoid a compile dependency on auth-service.
 */
@Value
@Builder
public class AuthTokenRequest {

    @JsonProperty("userId")
    Long userId;

    @JsonProperty("email")
    String email;

    @JsonProperty("roles")
    List<String> roles;

    /** Optional; when set, auth-service adds tenantId to JWT for tenant-scoped access. */
    @JsonProperty("tenantId")
    Long tenantId;
}
