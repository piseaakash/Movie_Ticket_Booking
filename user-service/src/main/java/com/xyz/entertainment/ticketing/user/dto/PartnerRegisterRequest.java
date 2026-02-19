package com.xyz.entertainment.ticketing.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerRegisterRequest {

    @Email
    @NotBlank
    @Size(max = 320)
    String email;

    @NotBlank
    @Size(max = 200)
    String fullName;

    @NotBlank
    @Size(min = 8, max = 72)
    String password;

    /** Display name for the partner tenant (e.g. theatre or chain name). */
    @NotBlank
    @Size(max = 200)
    String tenantName;

    /** Optional organisation (cinema chain) name; if provided, an Organisation is created and linked to the tenant. */
    @Size(max = 200)
    String organisationName;
}
