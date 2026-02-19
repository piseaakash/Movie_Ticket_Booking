package com.xyz.entertainment.ticketing.theatre.service;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateScreenRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.ScreenResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.UpdateScreenRequest;
import com.xyz.entertainment.ticketing.theatre.domain.Screen;
import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
import com.xyz.entertainment.ticketing.theatre.repository.ScreenRepository;
import com.xyz.entertainment.ticketing.theatre.repository.TheatreRepository;
import com.xyz.entertainment.ticketing.theatre.security.TenantContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private static final String ROLE_PARTNER_ADMIN = "PARTNER_ADMIN";
    private static final String ROLE_THEATRE_MANAGER = "THEATRE_MANAGER";

    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;

    private static void requirePartnerRole() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null || !ctx.hasAnyRole(ROLE_PARTNER_ADMIN, ROLE_THEATRE_MANAGER)) {
            throw new ForbiddenException("Partner role required");
        }
    }

    private Long requireTenantId() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null || ctx.getTenantId() == null) {
            throw new ForbiddenException("Tenant context required");
        }
        return ctx.getTenantId();
    }

    @Transactional
    public ScreenResponse createScreen(Long theatreId, CreateScreenRequest request) {
        requirePartnerRole();
        Long tenantId = requireTenantId();

        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }

        int displayOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : 0;
        Instant now = Instant.now();
        Screen screen = Screen.builder()
                .theatreId(theatreId)
                .name(request.getName().trim())
                .displayOrder(displayOrder)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Screen saved = screenRepository.save(screen);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> listScreens(Long theatreId) {
        Long tenantId = requireTenantId();
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }
        return screenRepository.findByTheatreIdOrderByDisplayOrderAsc(theatreId).stream()
                .map(ScreenService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScreenResponse getScreen(Long theatreId, Long screenId) {
        Long tenantId = requireTenantId();
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ScreenNotFoundException("Screen not found"));
        if (!screen.getTheatreId().equals(theatreId)) {
            throw new ScreenNotFoundException("Screen not found");
        }
        return toResponse(screen);
    }

    @Transactional
    public ScreenResponse updateScreen(Long theatreId, Long screenId, UpdateScreenRequest request) {
        requirePartnerRole();
        ScreenResponse current = getScreen(theatreId, screenId);
        Screen screen = screenRepository.findById(screenId).orElseThrow(() -> new ScreenNotFoundException("Screen not found"));
        screen.applyUpdate(request.getName(), request.getDisplayOrder());
        return toResponse(screen);
    }

    @Transactional
    public void deleteScreen(Long theatreId, Long screenId) {
        requirePartnerRole();
        getScreen(theatreId, screenId); // validates tenant
        screenRepository.deleteById(screenId);
    }

    private static ScreenResponse toResponse(Screen screen) {
        return new ScreenResponse()
                .id(screen.getId())
                .theatreId(screen.getTheatreId())
                .name(screen.getName())
                .displayOrder(screen.getDisplayOrder())
                .createdAt(screen.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(screen.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
}
