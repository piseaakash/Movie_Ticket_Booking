package com.xyz.entertainment.ticketing.booking.service;

import com.xyz.entertainment.ticketing.booking.api.model.BookingResponse;
import com.xyz.entertainment.ticketing.booking.api.model.BulkCancelRequest;
import com.xyz.entertainment.ticketing.booking.api.model.BulkCancelResponse;
import com.xyz.entertainment.ticketing.booking.api.model.CreateBookingRequest;
import com.xyz.entertainment.ticketing.booking.client.SeatsNotAvailableException;
import com.xyz.entertainment.ticketing.booking.client.TheatreSeatClient;
import com.xyz.entertainment.ticketing.booking.domain.Booking;
import com.xyz.entertainment.ticketing.booking.repository.BookingRepository;
import com.xyz.entertainment.ticketing.booking.security.CustomerContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int DEFAULT_LOCK_TTL_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final TheatreSeatClient theatreSeatClient;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Long userId = requireUserId();

        int ttlMinutes = request.getLockTtlMinutes() != null
                ? Math.min(30, Math.max(1, request.getLockTtlMinutes()))
                : DEFAULT_LOCK_TTL_MINUTES;
        Instant now = Instant.now();
        Instant reservedUntil = now.plusSeconds(ttlMinutes * 60L);

        BigDecimal totalPrice = request.getTotalPrice() != null
                ? BigDecimal.valueOf(request.getTotalPrice().orElse(0.0))
                : null;

        Booking booking = Booking.builder()
                .userId(userId)
                .showId(request.getShowId())
                .tenantId(request.getTenantId())
                .theatreId(request.getTheatreId() != null ? request.getTheatreId().orElse(null) : null)
                .seats(request.getSeats())
                .totalPrice(totalPrice)
                .status(Booking.Status.RESERVED)
                .createdAt(now)
                .reservedUntil(reservedUntil)
                .build();

        Booking saved = bookingRepository.save(booking);
        try {
            theatreSeatClient.lockSeats(saved.getShowId(), saved.getId(), saved.getSeats(), ttlMinutes);
        } catch (SeatsNotAvailableException e) {
            bookingRepository.delete(saved);
            throw e;
        }
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse confirmBooking(Long id) {
        Long userId = requireUserId();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenBookingAccessException("Booking does not belong to current user");
        }
        if (booking.getStatus() != Booking.Status.RESERVED) {
            throw new IllegalStateException("Booking is not in RESERVED state");
        }
        theatreSeatClient.confirmSeats(booking.getShowId(), booking.getId());
        Booking updated = Booking.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .tenantId(booking.getTenantId())
                .theatreId(booking.getTheatreId())
                .seats(booking.getSeats())
                .totalPrice(booking.getTotalPrice())
                .status(Booking.Status.CONFIRMED)
                .createdAt(booking.getCreatedAt())
                .reservedUntil(null)
                .build();
        bookingRepository.save(updated);
        return toResponse(updated);
    }

    @Transactional
    public List<BookingResponse> listBookingsForCurrentUser() {
        Long userId = requireUserId();
        List<Booking> list = bookingRepository.findByUserId(userId);
        List<BookingResponse> result = new ArrayList<>();
        for (Booking b : list) {
            result.add(toResponse(maybeExpireReservation(b)));
        }
        return result;
    }

    @Transactional
    public BookingResponse getBooking(Long id) {
        Long userId = requireUserId();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenBookingAccessException("Booking does not belong to current user");
        }
        return toResponse(maybeExpireReservation(booking));
    }

    /**
     * If booking is RESERVED and past reservedUntil, release seats and mark EXPIRED (lazy expiry).
     */
    private Booking maybeExpireReservation(Booking booking) {
        if (booking.getStatus() != Booking.Status.RESERVED
                || booking.getReservedUntil() == null
                || !booking.getReservedUntil().isBefore(Instant.now())) {
            return booking;
        }
        try {
            theatreSeatClient.releaseSeats(booking.getShowId(), booking.getId());
        } catch (Exception ignored) {
            // Best effort; theatre may have already released
        }
        Booking expired = Booking.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .tenantId(booking.getTenantId())
                .theatreId(booking.getTheatreId())
                .seats(booking.getSeats())
                .totalPrice(booking.getTotalPrice())
                .status(Booking.Status.EXPIRED)
                .createdAt(booking.getCreatedAt())
                .reservedUntil(null)
                .build();
        return bookingRepository.save(expired);
    }

    @Transactional
    public void cancelBooking(Long id) {
        Long userId = requireUserId();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenBookingAccessException("Booking does not belong to current user");
        }
        if (booking.getStatus() != Booking.Status.RESERVED && booking.getStatus() != Booking.Status.CONFIRMED) {
            return; // Idempotent: already CANCELLED or EXPIRED
        }
        try {
            theatreSeatClient.releaseSeats(booking.getShowId(), booking.getId());
        } catch (Exception ignored) {
            // Best effort
        }
        Booking cancelled = Booking.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .tenantId(booking.getTenantId())
                .theatreId(booking.getTheatreId())
                .seats(booking.getSeats())
                .totalPrice(booking.getTotalPrice())
                .status(Booking.Status.CANCELLED)
                .createdAt(booking.getCreatedAt())
                .reservedUntil(null)
                .build();
        bookingRepository.save(cancelled);
    }

    @Transactional
    public BulkCancelResponse bulkCancelBookings(BulkCancelRequest request) {
        Long userId = requireUserId();
        List<Long> cancelledIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        for (Long id : request.getBookingIds()) {
            try {
                Booking booking = bookingRepository.findById(id)
                        .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
                if (!booking.getUserId().equals(userId)) {
                    failedIds.add(id);
                    errors.put(id.toString(), "Booking does not belong to current user");
                    continue;
                }
                if (booking.getStatus() != Booking.Status.RESERVED && booking.getStatus() != Booking.Status.CONFIRMED) {
                    cancelledIds.add(id); // Idempotent: treat as cancelled
                    continue;
                }
                try {
                    theatreSeatClient.releaseSeats(booking.getShowId(), booking.getId());
                } catch (Exception ignored) {
                }
                Booking cancelled = Booking.builder()
                        .id(booking.getId())
                        .userId(booking.getUserId())
                        .showId(booking.getShowId())
                        .tenantId(booking.getTenantId())
                        .theatreId(booking.getTheatreId())
                        .seats(booking.getSeats())
                        .totalPrice(booking.getTotalPrice())
                        .status(Booking.Status.CANCELLED)
                        .createdAt(booking.getCreatedAt())
                        .reservedUntil(null)
                        .build();
                bookingRepository.save(cancelled);
                cancelledIds.add(id);
            } catch (BookingNotFoundException e) {
                failedIds.add(id);
                errors.put(id.toString(), "Booking not found");
            } catch (Exception e) {
                failedIds.add(id);
                errors.put(id.toString(), e.getMessage() != null ? e.getMessage() : "Cancel failed");
            }
        }
        return new BulkCancelResponse()
                .cancelledIds(cancelledIds)
                .failedIds(failedIds)
                .errors(errors);
    }

    private Long requireUserId() {
        Long userId = CustomerContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return userId;
    }

    private static BookingResponse toResponse(Booking booking) {
        BookingResponse resp = new BookingResponse()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .tenantId(booking.getTenantId())
                .status(BookingResponse.StatusEnum.valueOf(booking.getStatus().name()))
                .createdAt(booking.getCreatedAt().atOffset(java.time.ZoneOffset.UTC));
        if (booking.getTheatreId() != null) {
            resp.setTheatreId(org.openapitools.jackson.nullable.JsonNullable.of(booking.getTheatreId()));
        }
        if (booking.getSeats() != null) {
            resp.setSeats(booking.getSeats());
        }
        if (booking.getTotalPrice() != null) {
            resp.setTotalPrice(org.openapitools.jackson.nullable.JsonNullable.of(booking.getTotalPrice().doubleValue()));
        }
        if (booking.getReservedUntil() != null) {
            resp.setReservedUntil(org.openapitools.jackson.nullable.JsonNullable.of(booking.getReservedUntil().atOffset(java.time.ZoneOffset.UTC)));
        }
        return resp;
    }
}
