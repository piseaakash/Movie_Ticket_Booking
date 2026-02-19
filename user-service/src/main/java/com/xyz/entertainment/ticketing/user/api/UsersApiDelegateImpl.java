package com.xyz.entertainment.ticketing.user.api;

import com.xyz.entertainment.ticketing.user.api.model.LoginRequest;
import com.xyz.entertainment.ticketing.user.api.model.LoginResponse;
import com.xyz.entertainment.ticketing.user.api.model.RegisterUserRequest;
import com.xyz.entertainment.ticketing.user.api.model.UserResponse;
import com.xyz.entertainment.ticketing.user.client.AuthClient;
import com.xyz.entertainment.ticketing.user.service.UserAccountService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Delegate implementation that bridges generated API layer to the domain service.
 *
 * The OpenAPI generator will create the {@code UsersApiDelegate} interface and
 * a controller that forwards calls to this bean.
 */
@Component
@Primary
@RequiredArgsConstructor
public class UsersApiDelegateImpl implements UsersApiDelegate {

    private final UserAccountService userAccountService;
    private final AuthClient authClient;

    @Override
    public ResponseEntity<UserResponse> registerUser(RegisterUserRequest registerUserRequest) {
        var domainRequest = com.xyz.entertainment.ticketing.user.dto.RegisterUserRequest.builder()
                .email(registerUserRequest.getEmail())
                .fullName(registerUserRequest.getFullName())
                .password(registerUserRequest.getPassword())
                .build();

        var domainResponse = userAccountService.register(domainRequest);

        var apiResponse = new UserResponse()
                .id(domainResponse.getId())
                .email(domainResponse.getEmail())
                .fullName(domainResponse.getFullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Override
    public ResponseEntity<LoginResponse> loginUser(LoginRequest loginRequest) {
        var domainRequest = com.xyz.entertainment.ticketing.user.dto.LoginRequest.builder()
                .email(loginRequest.getEmail())
                .password(loginRequest.getPassword())
                .build();

        var domainResponse = userAccountService.login(domainRequest);

        var user = new UserResponse()
                .id(domainResponse.getId())
                .email(domainResponse.getEmail())
                .fullName(domainResponse.getFullName());

        var tokens = authClient.issueTokens(
                domainResponse.getId(),
                domainResponse.getEmail(),
                List.of("CUSTOMER"),
                null);

        var loginResponse = new LoginResponse()
                .user(user)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresInSeconds(tokens.getExpiresInSeconds());

        return ResponseEntity.ok(loginResponse);
    }
}

