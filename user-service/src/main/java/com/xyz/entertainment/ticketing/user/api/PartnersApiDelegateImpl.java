package com.xyz.entertainment.ticketing.user.api;

import com.xyz.entertainment.ticketing.user.api.model.LoginResponse;
import com.xyz.entertainment.ticketing.user.api.model.PartnerLoginRequest;
import com.xyz.entertainment.ticketing.user.api.model.PartnerRegisterRequest;
import com.xyz.entertainment.ticketing.user.api.model.PartnerRegisterResponse;
import com.xyz.entertainment.ticketing.user.api.model.UserResponse;
import com.xyz.entertainment.ticketing.user.service.PartnerLoginService;
import com.xyz.entertainment.ticketing.user.service.PartnerRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Delegate implementation for partner registration and login. Bridges the generated API
 * to {@link PartnerRegistrationService} and {@link PartnerLoginService}.
 */
@Component
@Primary
@RequiredArgsConstructor
public class PartnersApiDelegateImpl implements PartnersApiDelegate {

    private final PartnerRegistrationService partnerRegistrationService;
    private final PartnerLoginService partnerLoginService;

    @Override
    public ResponseEntity<PartnerRegisterResponse> registerPartner(PartnerRegisterRequest partnerRegisterRequest) {
        String organisationName = partnerRegisterRequest.getOrganisationName() != null
                ? partnerRegisterRequest.getOrganisationName().orElse(null)
                : null;

        var domainRequest = com.xyz.entertainment.ticketing.user.dto.PartnerRegisterRequest.builder()
                .email(partnerRegisterRequest.getEmail())
                .fullName(partnerRegisterRequest.getFullName())
                .password(partnerRegisterRequest.getPassword())
                .tenantName(partnerRegisterRequest.getTenantName())
                .organisationName(organisationName)
                .build();

        var result = partnerRegistrationService.registerPartner(domainRequest);

        var userResponse = new UserResponse()
                .id(result.getUser().getId())
                .email(result.getUser().getEmail())
                .fullName(result.getUser().getFullName());

        var apiResponse = new PartnerRegisterResponse()
                .user(userResponse)
                .tenantId(result.getTenantId())
                .tenantName(result.getTenantName())
                .role(result.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Override
    public ResponseEntity<LoginResponse> loginPartner(PartnerLoginRequest partnerLoginRequest) {
        Long tenantId = partnerLoginRequest.getTenantId() != null
                ? partnerLoginRequest.getTenantId().orElse(null)
                : null;

        var domainRequest = com.xyz.entertainment.ticketing.user.dto.PartnerLoginRequest.builder()
                .email(partnerLoginRequest.getEmail())
                .password(partnerLoginRequest.getPassword())
                .tenantId(tenantId)
                .build();

        var result = partnerLoginService.login(domainRequest);

        var userResponse = new UserResponse()
                .id(result.getUser().getId())
                .email(result.getUser().getEmail())
                .fullName(result.getUser().getFullName());

        var loginResponse = new LoginResponse()
                .user(userResponse)
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .expiresInSeconds(result.getExpiresInSeconds());

        return ResponseEntity.ok(loginResponse);
    }
}
