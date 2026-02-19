package com.xyz.entertainment.ticketing.theatre.service;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateTheatreRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.TheatreResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.UpdateTheatreRequest;
import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
import com.xyz.entertainment.ticketing.theatre.repository.TheatreRepository;
import com.xyz.entertainment.ticketing.theatre.security.TenantContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TheatreService {

    private static final String ROLE_PARTNER_ADMIN = "PARTNER_ADMIN";
    private static final String ROLE_THEATRE_MANAGER = "THEATRE_MANAGER";

    private final TheatreRepository theatreRepository;

    private static void requirePartnerRole() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null || !ctx.hasAnyRole(ROLE_PARTNER_ADMIN, ROLE_THEATRE_MANAGER)) {
            throw new ForbiddenException("Partner role required");
        }
    }

    @Transactional
    public TheatreResponse createTheatre(CreateTheatreRequest request) {
        requirePartnerRole();
        TenantContext ctx = TenantContext.get();
        Long tenantId = ctx.getTenantId();

        Instant now = Instant.now();
        Theatre theatre = Theatre.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .addressLine1(request.getAddressLine1())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Theatre saved = theatreRepository.save(theatre);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> listTheatres() {
        TenantContext ctx = TenantContext.get();
        Long tenantId = ctx.getTenantId();
        return theatreRepository.findByTenantId(tenantId).stream()
                .map(TheatreService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatre(Long id) {
        TenantContext ctx = TenantContext.get();
        Long tenantId = ctx.getTenantId();

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }
        return toResponse(theatre);
    }

    @Transactional
    public TheatreResponse updateTheatre(Long id, UpdateTheatreRequest request) {
        requirePartnerRole();
        TenantContext ctx = TenantContext.get();
        Long tenantId = ctx.getTenantId();

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }

        theatre.applyUpdate(
                request.getName(),
                request.getAddressLine1(),
                request.getCity(),
                request.getState(),
                request.getPostalCode());

        return toResponse(theatre);
    }

    @Transactional
    public void deleteTheatre(Long id) {
        requirePartnerRole();
        TenantContext ctx = TenantContext.get();
        Long tenantId = ctx.getTenantId();

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }

        theatreRepository.delete(theatre);
    }

    private static TheatreResponse toResponse(Theatre theatre) {
        return new TheatreResponse()
                .id(theatre.getId())
                .tenantId(theatre.getTenantId())
                .name(theatre.getName())
                .addressLine1(theatre.getAddressLine1())
                .city(theatre.getCity())
                .state(theatre.getState())
                .postalCode(theatre.getPostalCode())
                .createdAt(OffsetDateTime.ofInstant(theatre.getCreatedAt(), ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.ofInstant(theatre.getUpdatedAt(), ZoneOffset.UTC));
    }
}

