package com.xyz.entertainment.ticketing.user.service;

import com.xyz.entertainment.ticketing.user.domain.Organisation;
import com.xyz.entertainment.ticketing.user.domain.Tenant;
import com.xyz.entertainment.ticketing.user.domain.TenantStatus;
import com.xyz.entertainment.ticketing.user.domain.TenantType;
import com.xyz.entertainment.ticketing.user.domain.UserAccount;
import com.xyz.entertainment.ticketing.user.domain.UserTenantRole;
import com.xyz.entertainment.ticketing.user.dto.PartnerRegisterRequest;
import com.xyz.entertainment.ticketing.user.dto.PartnerRegisterResponse;
import com.xyz.entertainment.ticketing.user.dto.UserResponse;
import com.xyz.entertainment.ticketing.user.repository.OrganisationRepository;
import com.xyz.entertainment.ticketing.user.repository.TenantRepository;
import com.xyz.entertainment.ticketing.user.repository.UserAccountRepository;
import com.xyz.entertainment.ticketing.user.repository.UserTenantRoleRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a partner: creates UserAccount (same table as B2C), a PARTNER tenant,
 * and a UserTenantRole linking the user to the tenant with PARTNER_ADMIN.
 * Optionally creates an Organisation when organisationName is provided.
 *
 * Tradeoff: User creation is duplicated here vs {@link UserAccountService#register}
 * so that a single transaction creates user, tenant, and role; otherwise a failed
 * tenant/role creation would leave an orphan B2C-style user.
 */
@Service
@RequiredArgsConstructor
public class PartnerRegistrationService {

    private static final String PARTNER_DEFAULT_ROLE = "PARTNER_ADMIN";

    private final UserAccountRepository userAccountRepository;
    private final OrganisationRepository organisationRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PartnerRegisterResponse registerPartner(PartnerRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email already exists");
        }

        Instant now = Instant.now();

        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        user = userAccountRepository.save(user);

        Organisation organisation = null;
        if (request.getOrganisationName() != null && !request.getOrganisationName().isBlank()) {
            organisation = Organisation.builder()
                    .name(request.getOrganisationName().trim())
                    .createdAt(now)
                    .build();
            organisation = organisationRepository.save(organisation);
        }

        Tenant tenant = Tenant.builder()
                .name(request.getTenantName().trim())
                .type(TenantType.PARTNER)
                .status(TenantStatus.ACTIVE)
                .organisation(organisation)
                .createdAt(now)
                .build();
        tenant = tenantRepository.save(tenant);

        UserTenantRole role = UserTenantRole.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .role(PARTNER_DEFAULT_ROLE)
                .createdAt(now)
                .build();
        userTenantRoleRepository.save(role);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();

        return PartnerRegisterResponse.builder()
                .user(userResponse)
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .role(PARTNER_DEFAULT_ROLE)
                .build();
    }
}
