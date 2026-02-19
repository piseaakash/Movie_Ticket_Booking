package com.xyz.entertainment.ticketing.theatre.api;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateSeatRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.SeatResponse;
import com.xyz.entertainment.ticketing.theatre.service.SeatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class ScreensApiDelegateImpl implements ScreensApiDelegate {

    private final SeatService seatService;

    @Override
    public ResponseEntity<SeatResponse> createSeat(Long screenId, CreateSeatRequest createSeatRequest) {
        SeatResponse response = seatService.createSeat(screenId, createSeatRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> deleteSeat(Long screenId, Long seatId) {
        seatService.deleteSeat(screenId, seatId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<SeatResponse>> listSeats(Long screenId) {
        List<SeatResponse> list = seatService.listSeats(screenId);
        return ResponseEntity.ok(list);
    }
}
