package com.xyz.entertainment.ticketing.movie.service;

import com.xyz.entertainment.ticketing.movie.api.model.CreateShowRequest;
import com.xyz.entertainment.ticketing.movie.api.model.ShowResponse;
import com.xyz.entertainment.ticketing.movie.api.model.UpdateShowRequest;
import com.xyz.entertainment.ticketing.movie.domain.Show;
import com.xyz.entertainment.ticketing.movie.repository.MovieRepository;
import com.xyz.entertainment.ticketing.movie.repository.ShowRepository;
import com.xyz.entertainment.ticketing.movie.security.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Partner-only: create, update, delete shows for the day. All operations are scoped to the JWT tenant.
 */
@Service
@RequiredArgsConstructor
public class ShowManagementService {

    private static final String ROLE_PARTNER_ADMIN = "PARTNER_ADMIN";
    private static final String ROLE_THEATRE_MANAGER = "THEATRE_MANAGER";

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {
        Long tenantId = requireTenantId();
        requirePartnerRole();

        if (!movieRepository.existsById(request.getMovieId())) {
            throw new MovieNotFoundException("Movie not found");
        }

        Instant now = Instant.now();
        Show show = Show.builder()
                .movieId(request.getMovieId())
                .theatreId(request.getTheatreId())
                .tenantId(tenantId)
                .city(request.getCity().trim())
                .language(request.getLanguage() != null ? request.getLanguage().trim() : null)
                .startTime(request.getStartTime().toInstant())
                .basePrice(BigDecimal.valueOf(request.getBasePrice()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        Show saved = showRepository.save(show);
        return toResponse(saved);
    }

    @Transactional
    public ShowResponse updateShow(Long id, UpdateShowRequest request) {
        Long tenantId = requireTenantId();
        requirePartnerRole();

        Show existing = showRepository.findById(id)
                .orElseThrow(() -> new ShowNotFoundException("Show not found"));
        if (!existing.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }

        Instant startTime = request.getStartTime() != null
                ? request.getStartTime().toInstant()
                : existing.getStartTime();
        BigDecimal basePrice = request.getBasePrice() != null
                ? BigDecimal.valueOf(request.getBasePrice())
                : existing.getBasePrice();

        Show updated = Show.builder()
                .id(existing.getId())
                .movieId(existing.getMovieId())
                .theatreId(existing.getTheatreId())
                .tenantId(existing.getTenantId())
                .city(existing.getCity())
                .language(existing.getLanguage())
                .startTime(startTime)
                .basePrice(basePrice)
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        Show saved = showRepository.save(updated);
        return toResponse(saved);
    }

    @Transactional
    public void deleteShow(Long id) {
        Long tenantId = requireTenantId();
        requirePartnerRole();

        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ShowNotFoundException("Show not found"));
        if (!show.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }
        showRepository.deleteById(id);
    }

    private Long requireTenantId() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null || ctx.getTenantId() == null) {
            throw new ForbiddenException("Tenant context required");
        }
        return ctx.getTenantId();
    }

    private void requirePartnerRole() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null || !ctx.hasAnyRole(ROLE_PARTNER_ADMIN, ROLE_THEATRE_MANAGER)) {
            throw new ForbiddenException("Partner role required");
        }
    }

    private static ShowResponse toResponse(Show show) {
        return new ShowResponse()
                .id(show.getId())
                .movieId(show.getMovieId())
                .theatreId(show.getTheatreId())
                .tenantId(show.getTenantId())
                .city(show.getCity())
                .language(show.getLanguage())
                .startTime(show.getStartTime().atOffset(ZoneOffset.UTC))
                .basePrice(show.getBasePrice().doubleValue());
    }
}
