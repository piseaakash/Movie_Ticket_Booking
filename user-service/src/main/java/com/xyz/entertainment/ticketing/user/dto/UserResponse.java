package com.xyz.entertainment.ticketing.user.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {

    Long id;

    String email;

    String fullName;
}

