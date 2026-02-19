package com.xyz.entertainment.ticketing.user.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerRegisterResponse {

    UserResponse user;
    long tenantId;
    String tenantName;
    String role;
}
