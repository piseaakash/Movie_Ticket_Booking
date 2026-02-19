package com.xyz.entertainment.ticketing.booking.api;

import com.xyz.entertainment.ticketing.booking.api.model.BulkCancelRequest;
import com.xyz.entertainment.ticketing.booking.api.model.BulkCancelResponse;
import com.xyz.entertainment.ticketing.booking.api.model.BookingResponse;
import com.xyz.entertainment.ticketing.booking.api.model.ConfirmPaymentAndBookingRequest;
import com.xyz.entertainment.ticketing.booking.api.model.CreateBookingRequest;
import com.xyz.entertainment.ticketing.booking.api.model.ReserveBookingRequest;
import com.xyz.entertainment.ticketing.booking.api.model.ReserveBookingResponse;
import com.xyz.entertainment.ticketing.booking.service.BookingOrchestrationService;
import com.xyz.entertainment.ticketing.booking.service.BookingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class BookingsApiDelegateImpl implements BookingsApiDelegate {

    private final BookingService bookingService;
    private final BookingOrchestrationService bookingOrchestrationService;

    @Override
    public ResponseEntity<BookingResponse> createBooking(CreateBookingRequest createBookingRequest) {
        BookingResponse resp = bookingService.createBooking(createBookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Override
    public ResponseEntity<BookingResponse> confirmBooking(Long id) {
        BookingResponse resp = bookingService.confirmBooking(id);
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<BookingResponse> getBooking(Long id) {
        BookingResponse resp = bookingService.getBooking(id);
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<List<BookingResponse>> listBookings() {
        List<BookingResponse> list = bookingService.listBookingsForCurrentUser();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> cancelBooking(Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BulkCancelResponse> bulkCancelBookings(BulkCancelRequest bulkCancelRequest) {
        BulkCancelResponse resp = bookingService.bulkCancelBookings(bulkCancelRequest);
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<ReserveBookingResponse> reserveBooking(ReserveBookingRequest reserveBookingRequest) {
        CreateBookingRequest createReq = toCreateBookingRequest(reserveBookingRequest);
        double amount = reserveBookingRequest.getAmount() != null ? reserveBookingRequest.getAmount() : 0.0;
        String currency = reserveBookingRequest.getCurrency() != null ? reserveBookingRequest.getCurrency() : "INR";
        var result = bookingOrchestrationService.reserveBooking(createReq, amount, currency);
        ReserveBookingResponse resp = new ReserveBookingResponse()
                .booking(result.booking())
                .paymentId(result.paymentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Override
    public ResponseEntity<BookingResponse> confirmPaymentAndBooking(ConfirmPaymentAndBookingRequest confirmPaymentAndBookingRequest) {
        Long bookingId = confirmPaymentAndBookingRequest.getBookingId();
        Long paymentId = confirmPaymentAndBookingRequest.getPaymentId();
        BookingResponse resp = bookingOrchestrationService.confirmPaymentAndBooking(bookingId, paymentId);
        return ResponseEntity.ok(resp);
    }

    private static CreateBookingRequest toCreateBookingRequest(ReserveBookingRequest r) {
        CreateBookingRequest req = new CreateBookingRequest()
                .showId(r.getShowId())
                .tenantId(r.getTenantId())
                .seats(r.getSeats());
        if (r.getTheatreId() != null && r.getTheatreId().isPresent()) {
            req.setTheatreId(r.getTheatreId());
        }
        if (r.getTotalPrice() != null && r.getTotalPrice().isPresent()) {
            req.setTotalPrice(r.getTotalPrice());
        }
        if (r.getLockTtlMinutes() != null) {
            req.setLockTtlMinutes(r.getLockTtlMinutes());
        }
        return req;
    }
}

