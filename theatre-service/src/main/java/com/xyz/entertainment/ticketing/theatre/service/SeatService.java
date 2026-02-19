package com.xyz.entertainment.ticketing.theatre.service;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateSeatRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.SeatResponse;
import com.xyz.entertainment.ticketing.theatre.domain.Screen;
import com.xyz.entertainment.ticketing.theatre.domain.Seat;
import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
import com.xyz.entertainment.ticketing.theatre.repository.ScreenRepository;
import com.xyz.entertainment.ticketing.theatre.repository.SeatRepository;
import com.xyz.entertainment.ticketing.theatre.repository.TheatreRepository;
import com.xyz.entertainment.ticketing.theatre.security.TenantContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatService {

    private static final String ROLE_PARTNER_ADMIN = "PARTNER_ADMIN";
    private static final String ROLE_THEATRE_MANAGER = "THEATRE_MANAGER";

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;

    private static void requirePartnerRole() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null || !ctx.hasAnyRole(ROLE_PARTNER_ADMIN, ROLE_THEATRE_MANAGER)) {
            throw new ForbiddenException("Partner role required");
        }
    }

    private void requireScreenBelongsToTenant(Long screenId, Long tenantId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ScreenNotFoundException("Screen not found"));
        Theatre theatre = theatreRepository.findById(screen.getTheatreId())
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
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
    public SeatResponse createSeat(Long screenId, CreateSeatRequest request) {
        requirePartnerRole();
        Long tenantId = requireTenantId();
        requireScreenBelongsToTenant(screenId, tenantId);

        Seat seat = Seat.builder()
                .screenId(screenId)
                .rowLabel(request.getRowLabel().trim())
                .seatNumber(request.getSeatNumber())
                .label(request.getLabel().trim())
                .build();
        Seat saved = seatRepository.save(seat);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> listSeats(Long screenId) {
        Long tenantId = requireTenantId();
        requireScreenBelongsToTenant(screenId, tenantId);

        return seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId).stream()
                .map(SeatService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSeat(Long screenId, Long seatId) {
        requirePartnerRole();
        Long tenantId = requireTenantId();
        requireScreenBelongsToTenant(screenId, tenantId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException("Seat not found"));
        if (!seat.getScreenId().equals(screenId)) {
            throw new SeatNotFoundException("Seat not found");
        }
        seatRepository.delete(seat);
    }

    private static SeatResponse toResponse(Seat seat) {
        return new SeatResponse()
                .id(seat.getId())
                .screenId(seat.getScreenId())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .label(seat.getLabel());
    }
}
