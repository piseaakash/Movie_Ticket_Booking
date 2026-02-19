package com.xyz.entertainment.ticketing.user.service;

import com.xyz.entertainment.ticketing.user.client.AuthClient;
import com.xyz.entertainment.ticketing.user.domain.Tenant;
import com.xyz.entertainment.ticketing.user.domain.TenantStatus;
import com.xyz.entertainment.ticketing.user.domain.TenantType;
import com.xyz.entertainment.ticketing.user.domain.UserTenantRole;
import com.xyz.entertainment.ticketing.user.dto.LoginRequest;
import com.xyz.entertainment.ticketing.user.dto.PartnerLoginRequest;
import com.xyz.entertainment.ticketing.user.dto.PartnerLoginResult;
import com.xyz.entertainment.ticketing.user.dto.UserResponse;
import com.xyz.entertainment.ticketing.user.repository.TenantRepository;
import com.xyz.entertainment.ticketing.user.repository.UserTenantRoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Partner login: validates credentials (same as B2C), resolves tenant from
 * request tenantId or single PARTNER ACTIVE tenant, enforces PENDING_APPROVAL,
 * resolves roles from UserTenantRole, and issues tokens (with tenantId claim) via auth-service.
 */
@Service
@RequiredArgsConstructor
public class PartnerLoginService {

    private final UserAccountService userAccountService;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final TenantRepository tenantRepository;
    private final AuthClient authClient;

    @Transactional(readOnly = true)
    public PartnerLoginResult login(PartnerLoginRequest request) {
        UserResponse user = userAccountService.login(
                LoginRequest.builder()
                        .email(request.getEmail())
                        .password(request.getPassword())
                        .build());

        Long requestedTenantId = request.getTenantId();
        UserTenantRole utr;
        Tenant tenant;

        if (requestedTenantId != null) {
            utr = userTenantRoleRepository.findByUserIdAndTenantId(user.getId(), requestedTenantId)
                    .orElseThrow(() -> new TenantRequiredException("Not a member of this tenant"));
            tenant = tenantRepository.findById(requestedTenantId)
                    .orElseThrow(() -> new TenantRequiredException("Tenant not found"));
        } else {
            List<UserTenantRole> list = userTenantRoleRepository.findByUserId(user.getId());
            if (list.isEmpty()) {
                throw new TenantRequiredException("tenant required");
            }
            List<Long> tenantIds = list.stream().map(UserTenantRole::getTenantId).distinct().toList();
            List<Tenant> tenants = tenantRepository.findAllById(tenantIds);
            List<Tenant> partnerActive = tenants.stream()
                    .filter(t -> t.getType() == TenantType.PARTNER && t.getStatus() == TenantStatus.ACTIVE)
                    .toList();
            if (partnerActive.isEmpty()) {
                throw new TenantRequiredException("tenant required");
            }
            if (partnerActive.size() > 1) {
                throw new TenantRequiredException("tenant required");
            }
            tenant = partnerActive.get(0);
            utr = list.stream()
                    .filter(r -> r.getTenantId().equals(tenant.getId()))
                    .findFirst()
                    .orElseThrow(() -> new TenantRequiredException("tenant required"));
        }

        if (tenant.getStatus() == TenantStatus.PENDING_APPROVAL) {
            throw new PartnerPendingApprovalException("Partner account pending approval");
        }
        if (tenant.getType() != TenantType.PARTNER) {
            throw new TenantRequiredException("tenant required");
        }

        List<String> roles = List.of(utr.getRole());
        var tokens = authClient.issueTokens(user.getId(), user.getEmail(), roles, tenant.getId());

        return PartnerLoginResult.builder()
                .user(user)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresInSeconds(tokens.getExpiresInSeconds())
                .build();
    }
}
