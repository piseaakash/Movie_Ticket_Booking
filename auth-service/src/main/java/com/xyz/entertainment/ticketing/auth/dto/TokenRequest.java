package com.xyz.entertainment.ticketing.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Request from another service (e.g. user-service) asking to
 * issue tokens for an already-authenticated user.
 */
@Value
@Builder
public class TokenRequest {

    @NotNull
    Long userId;

    @NotBlank
    @Size(max = 320)
    String email;

    /**
     * Roles for the user, e.g. CUSTOMER, PARTNER_ADMIN.
     */
    List<@NotBlank String> roles;

    /**
     * Optional tenant id for B2B; when set, added as JWT claim so APIs can scope access by tenant.
     */
    Long tenantId;
}

