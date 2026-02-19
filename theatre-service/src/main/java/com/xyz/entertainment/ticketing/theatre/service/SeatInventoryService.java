package com.xyz.entertainment.ticketing.theatre.service;

import com.xyz.entertainment.ticketing.theatre.api.model.ConfirmLockRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.LockSeatsRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.RegisterShowRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.ReleaseSeatsRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.SeatAvailabilityResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.ShowScreenResponse;
import com.xyz.entertainment.ticketing.theatre.domain.Screen;
import com.xyz.entertainment.ticketing.theatre.domain.Seat;
import com.xyz.entertainment.ticketing.theatre.domain.ShowScreen;
import com.xyz.entertainment.ticketing.theatre.domain.ShowSeat;
import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
import com.xyz.entertainment.ticketing.theatre.repository.ScreenRepository;
import com.xyz.entertainment.ticketing.theatre.repository.SeatRepository;
import com.xyz.entertainment.ticketing.theatre.repository.ShowScreenRepository;
import com.xyz.entertainment.ticketing.theatre.repository.ShowSeatRepository;
import com.xyz.entertainment.ticketing.theatre.repository.TheatreRepository;
import com.xyz.entertainment.ticketing.theatre.security.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatInventoryService {

    private static final String ROLE_PARTNER_ADMIN = "PARTNER_ADMIN";
    private static final String ROLE_THEATRE_MANAGER = "THEATRE_MANAGER";
    private static final int DEFAULT_LOCK_TTL_MINUTES = 10;

    private final ShowScreenRepository showScreenRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;

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
    public ShowScreenResponse registerShowToScreen(Long showId, RegisterShowRequest request) {
        requirePartnerRole();
        Long tenantId = requireTenantId();

        if (showScreenRepository.findByShowId(showId).isPresent()) {
            throw new IllegalArgumentException("Show already registered to a screen");
        }

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ScreenNotFoundException("Screen not found"));
        Theatre theatre = theatreRepository.findById(screen.getTheatreId())
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found"));
        if (!theatre.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Tenant mismatch");
        }

        ShowScreen showScreen = ShowScreen.builder()
                .showId(showId)
                .screenId(screen.getId())
                .build();
        showScreenRepository.save(showScreen);

        List<Seat> seats = seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screen.getId());
        Instant now = Instant.now();
        for (Seat seat : seats) {
            ShowSeat ss = ShowSeat.builder()
                    .showId(showId)
                    .seatId(seat.getId())
                    .status(ShowSeat.Status.AVAILABLE)
                    .build();
            showSeatRepository.save(ss);
        }

        return new ShowScreenResponse().showId(showId).screenId(screen.getId());
    }

    @Transactional(readOnly = true)
    public ShowScreenResponse getShowScreen(Long showId) {
        ShowScreen showScreen = showScreenRepository.findByShowId(showId)
                .orElseThrow(() -> new ShowNotRegisteredException("Show not registered"));
        return new ShowScreenResponse().showId(showId).screenId(showScreen.getScreenId());
    }

    @Transactional(readOnly = true)
    public List<SeatAvailabilityResponse> getShowSeatAvailability(Long showId) {
        ShowScreen showScreen = showScreenRepository.findByShowId(showId)
                .orElseThrow(() -> new ShowNotRegisteredException("Show not registered"));

        // Release expired locks so availability is accurate
        showSeatRepository.releaseExpiredLocks(Instant.now());

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        List<Long> seatIds = showSeats.stream().map(ShowSeat::getSeatId).distinct().toList();
        Map<Long, Seat> seatMap = seatRepository.findAllById(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, s -> s));

        return showSeats.stream()
                .map(ss -> {
                    ShowSeat.Status status = ss.getStatus();
                    if (status == ShowSeat.Status.LOCKED && ss.getLockedUntil() != null && ss.getLockedUntil().isBefore(Instant.now())) {
                        status = ShowSeat.Status.AVAILABLE;
                    }
                    Seat seat = seatMap.get(ss.getSeatId());
                    String label = seat != null ? seat.getLabel() : "?";
                    return new SeatAvailabilityResponse()
                            .seatId(ss.getSeatId())
                            .label(label)
                            .status(SeatAvailabilityResponse.StatusEnum.valueOf(status.name()));
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void lockSeats(Long showId, LockSeatsRequest request) {
        ShowScreen showScreen = showScreenRepository.findByShowId(showId)
                .orElseThrow(() -> new ShowNotRegisteredException("Show not registered"));

        showSeatRepository.releaseExpiredLocks(Instant.now());

        List<Seat> seats = seatRepository.findByScreenIdAndLabelIn(showScreen.getScreenId(), request.getSeatLabels());
        if (seats.size() != request.getSeatLabels().size()) {
            throw new SeatsNotAvailableException("One or more seat labels not found or duplicate");
        }
        List<Long> seatIds = seats.stream().map(Seat::getId).toList();

        int ttlMinutes = request.getLockTtlMinutes() != null
                ? Math.min(30, Math.max(1, request.getLockTtlMinutes()))
                : DEFAULT_LOCK_TTL_MINUTES;
        Instant lockedUntil = Instant.now().plusSeconds(ttlMinutes * 60L);

        int updated = showSeatRepository.lockAvailableSeats(showId, seatIds, request.getBookingId(), lockedUntil);
        if (updated != seatIds.size()) {
            throw new SeatsNotAvailableException("One or more seats are not available for lock");
        }
    }

    @Transactional
    public void confirmSeatLock(Long showId, ConfirmLockRequest request) {
        if (showScreenRepository.findByShowId(showId).isEmpty()) {
            throw new ShowNotRegisteredException("Show not registered");
        }
        int updated = showSeatRepository.confirmLockedSeats(showId, request.getBookingId());
        if (updated == 0) {
            throw new ShowNotRegisteredException("No locked seats found for this booking");
        }
    }

    @Transactional
    public void releaseSeatLock(Long showId, ReleaseSeatsRequest request) {
        if (showScreenRepository.findByShowId(showId).isEmpty()) {
            throw new ShowNotRegisteredException("Show not registered");
        }
        showSeatRepository.releaseSeatsByBooking(showId, request.getBookingId());
    }
}
