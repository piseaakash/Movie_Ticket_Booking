package com.xyz.entertainment.ticketing.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerLoginRequest {

    @Email
    @NotBlank
    @Size(max = 320)
    String email;

    @NotBlank
    @Size(min = 8, max = 72)
    String password;

    /** Optional; when omitted, a single PARTNER ACTIVE tenant is used or "tenant required" is returned. */
    Long tenantId;
}
