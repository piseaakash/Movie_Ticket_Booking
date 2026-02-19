package com.xyz.entertainment.ticketing.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenResponse {

    String accessToken;

    String refreshToken;

    long expiresInSeconds;
}

