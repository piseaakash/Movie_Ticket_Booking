package com.xyz.entertainment.ticketing.user.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerLoginResult {

    UserResponse user;
    String accessToken;
    String refreshToken;
    long expiresInSeconds;
}
