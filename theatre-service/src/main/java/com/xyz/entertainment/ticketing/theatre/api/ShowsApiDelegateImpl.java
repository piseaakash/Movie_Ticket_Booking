package com.xyz.entertainment.ticketing.theatre.api;

import com.xyz.entertainment.ticketing.theatre.api.model.ConfirmLockRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.LockSeatsRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.RegisterShowRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.ReleaseSeatsRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.SeatAvailabilityResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.ShowScreenResponse;
import com.xyz.entertainment.ticketing.theatre.service.SeatInventoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class ShowsApiDelegateImpl implements ShowsApiDelegate {

    private final SeatInventoryService seatInventoryService;

    @Override
    public ResponseEntity<Void> confirmSeatLock(Long showId, ConfirmLockRequest confirmLockRequest) {
        seatInventoryService.confirmSeatLock(showId, confirmLockRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ShowScreenResponse> getShowScreen(Long showId) {
        ShowScreenResponse response = seatInventoryService.getShowScreen(showId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<SeatAvailabilityResponse>> getShowSeatAvailability(Long showId) {
        List<SeatAvailabilityResponse> list = seatInventoryService.getShowSeatAvailability(showId);
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> lockSeats(Long showId, LockSeatsRequest lockSeatsRequest) {
        seatInventoryService.lockSeats(showId, lockSeatsRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ShowScreenResponse> registerShowToScreen(Long showId, RegisterShowRequest registerShowRequest) {
        ShowScreenResponse response = seatInventoryService.registerShowToScreen(showId, registerShowRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> releaseSeatLock(Long showId, ReleaseSeatsRequest releaseSeatsRequest) {
        seatInventoryService.releaseSeatLock(showId, releaseSeatsRequest);
        return ResponseEntity.noContent().build();
    }
}
