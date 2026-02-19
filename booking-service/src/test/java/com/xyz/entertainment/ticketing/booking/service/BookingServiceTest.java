package com.xyz.entertainment.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.booking.api.model.BulkCancelRequest;
import com.xyz.entertainment.ticketing.booking.api.model.BulkCancelResponse;
import com.xyz.entertainment.ticketing.booking.api.model.BookingResponse;
import com.xyz.entertainment.ticketing.booking.api.model.CreateBookingRequest;
import com.xyz.entertainment.ticketing.booking.client.SeatsNotAvailableException;
import com.xyz.entertainment.ticketing.booking.client.TheatreSeatClient;
import com.xyz.entertainment.ticketing.booking.domain.Booking;
import com.xyz.entertainment.ticketing.booking.repository.BookingRepository;
import com.xyz.entertainment.ticketing.booking.security.CustomerContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TheatreSeatClient theatreSeatClient;

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    private BookingService service() {
        return new BookingService(bookingRepository, theatreSeatClient);
    }

    private Booking booking(Long id, Long userId) {
        return booking(id, userId, Booking.Status.CONFIRMED, null);
    }

    private Booking booking(Long id, Long userId, Booking.Status status, Instant reservedUntil) {
        return Booking.builder()
                .id(id)
                .userId(userId)
                .showId(1L)
                .tenantId(10L)
                .theatreId(5L)
                .seats(List.of("A1", "A2"))
                .totalPrice(BigDecimal.TEN)
                .status(status)
                .createdAt(Instant.now())
                .reservedUntil(reservedUntil)
                .build();
    }

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        @Test
        @DisplayName("creates booking for current user with totalPrice and theatreId")
        void createBooking_withPriceAndTheatre() {
            CustomerContext.setUserId(99L);
            CreateBookingRequest req = new CreateBookingRequest()
                    .showId(1L)
                    .tenantId(10L)
                    .theatreId(5L)
                    .seats(List.of("A1", "A2"))
                    .totalPrice(100.0);

            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                return Booking.builder()
                        .id(1L)
                        .userId(b.getUserId())
                        .showId(b.getShowId())
                        .tenantId(b.getTenantId())
                        .theatreId(b.getTheatreId())
                        .seats(b.getSeats())
                        .totalPrice(b.getTotalPrice())
                        .status(b.getStatus())
                        .createdAt(b.getCreatedAt())
                        .reservedUntil(b.getReservedUntil())
                        .build();
            });

            var svc = service();
            BookingResponse resp = svc.createBooking(req);

            assertThat(resp.getUserId()).isEqualTo(99L);
            assertThat(resp.getShowId()).isEqualTo(1L);
            assertThat(resp.getTenantId()).isEqualTo(10L);
            assertThat(resp.getSeats()).containsExactly("A1", "A2");
            assertThat(resp.getTotalPrice().get()).isEqualTo(100.0);
            assertThat(resp.getStatus()).isEqualTo(BookingResponse.StatusEnum.RESERVED);

            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(99L);
            verify(theatreSeatClient).lockSeats(1L, 1L, List.of("A1", "A2"), 15);
        }

        @Test
        @DisplayName("creates booking without optional totalPrice and theatreId")
        void createBooking_withoutOptionalFields() {
            CustomerContext.setUserId(99L);
            CreateBookingRequest req = new CreateBookingRequest()
                    .showId(1L)
                    .tenantId(10L)
                    .seats(List.of("A1", "A2"));

            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                return Booking.builder()
                        .id(2L)
                        .userId(b.getUserId())
                        .showId(b.getShowId())
                        .tenantId(b.getTenantId())
                        .theatreId(b.getTheatreId())
                        .seats(b.getSeats())
                        .totalPrice(b.getTotalPrice())
                        .status(b.getStatus())
                        .createdAt(b.getCreatedAt())
                        .reservedUntil(b.getReservedUntil())
                        .build();
            });

            var svc = service();
            BookingResponse resp = svc.createBooking(req);

            // When totalPrice is omitted, service uses orElse(0.0) so response has totalPrice present as 0.0
            assertThat(resp.getTotalPrice().isPresent()).isTrue();
            assertThat(resp.getTotalPrice().get()).isEqualTo(0.0);
            assertThat(resp.getTheatreId().isPresent()).isFalse();
            assertThat(resp.getStatus()).isEqualTo(BookingResponse.StatusEnum.RESERVED);
        }

        @Test
        @DisplayName("deletes booking and throws when lock fails (seats not available)")
        void createBooking_seatsNotAvailable() {
            CustomerContext.setUserId(99L);
            CreateBookingRequest req = new CreateBookingRequest()
                    .showId(1L)
                    .tenantId(10L)
                    .seats(List.of("A1", "A2"));

            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                return Booking.builder()
                        .id(10L)
                        .userId(b.getUserId())
                        .showId(b.getShowId())
                        .tenantId(b.getTenantId())
                        .theatreId(b.getTheatreId())
                        .seats(b.getSeats())
                        .totalPrice(b.getTotalPrice())
                        .status(b.getStatus())
                        .createdAt(b.getCreatedAt())
                        .reservedUntil(b.getReservedUntil())
                        .build();
            });
            doThrow(new SeatsNotAvailableException("Seats not available")).when(theatreSeatClient).lockSeats(1L, 10L, List.of("A1", "A2"), 15);

            var svc = service();
            assertThatThrownBy(() -> svc.createBooking(req))
                    .isInstanceOf(SeatsNotAvailableException.class);
            verify(bookingRepository).delete(any(Booking.class));
        }

        @Test
        @DisplayName("throws when no user in context")
        void createBooking_noUser() {
            var svc = service();
            assertThatThrownBy(() -> svc.createBooking(new CreateBookingRequest()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("listBookingsForCurrentUser")
    class ListBookings {

        @Test
        @DisplayName("lists bookings only for current user")
        void listBookings_ok() {
            CustomerContext.setUserId(50L);
            Booking b1 = booking(1L, 50L);
            Booking b2 = booking(2L, 50L);
            when(bookingRepository.findByUserId(50L)).thenReturn(List.of(b1, b2));

            var svc = service();
            List<BookingResponse> list = svc.listBookingsForCurrentUser();

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getUserId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("throws when no user in context")
        void listBookings_noUser() {
            var svc = service();
            assertThatThrownBy(svc::listBookingsForCurrentUser)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("expires RESERVED past reservedUntil when listing")
        void listBookings_expiresReserved() {
            CustomerContext.setUserId(50L);
            Instant past = Instant.now().minusSeconds(120);
            Booking reserved = booking(1L, 50L, Booking.Status.RESERVED, past);
            when(bookingRepository.findByUserId(50L)).thenReturn(List.of(reserved));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            var svc = service();
            List<BookingResponse> list = svc.listBookingsForCurrentUser();

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStatus()).isEqualTo(BookingResponse.StatusEnum.EXPIRED);
            verify(theatreSeatClient).releaseSeats(1L, 1L);
        }
    }

    @Nested
    @DisplayName("getBooking")
    class GetBooking {

        @Test
        @DisplayName("returns booking when owned by current user")
        void getBooking_ok() {
            CustomerContext.setUserId(77L);
            Booking b = booking(1L, 77L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));

            var svc = service();
            BookingResponse resp = svc.getBooking(1L);

            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getUserId()).isEqualTo(77L);
        }

        @Test
        @DisplayName("throws BookingNotFoundException when missing")
        void getBooking_notFound() {
            CustomerContext.setUserId(77L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

            var svc = service();
            assertThatThrownBy(() -> svc.getBooking(1L))
                    .isInstanceOf(BookingNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenBookingAccessException when not owned by current user")
        void getBooking_forbidden() {
            CustomerContext.setUserId(77L);
            Booking b = booking(1L, 99L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));

            var svc = service();
            assertThatThrownBy(() -> svc.getBooking(1L))
                    .isInstanceOf(ForbiddenBookingAccessException.class);
        }

        @Test
        @DisplayName("throws when no user in context")
        void getBooking_noUser() {
            var svc = service();
            assertThatThrownBy(() -> svc.getBooking(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("expires RESERVED booking when past reservedUntil and returns EXPIRED")
        void getBooking_expiresReserved() {
            CustomerContext.setUserId(77L);
            Instant past = Instant.now().minusSeconds(60);
            Booking reserved = booking(1L, 77L, Booking.Status.RESERVED, past);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reserved));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            var svc = service();
            BookingResponse resp = svc.getBooking(1L);

            assertThat(resp.getStatus()).isEqualTo(BookingResponse.StatusEnum.EXPIRED);
            verify(theatreSeatClient).releaseSeats(1L, 1L);
            verify(bookingRepository).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("confirmBooking")
    class ConfirmBooking {

        @Test
        @DisplayName("confirms RESERVED booking and returns CONFIRMED")
        void confirmBooking_ok() {
            CustomerContext.setUserId(50L);
            Booking reserved = booking(1L, 50L, Booking.Status.RESERVED, Instant.now().plusSeconds(600));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reserved));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            var svc = service();
            BookingResponse resp = svc.confirmBooking(1L);

            assertThat(resp.getStatus()).isEqualTo(BookingResponse.StatusEnum.CONFIRMED);
            verify(theatreSeatClient).confirmSeats(1L, 1L);
        }

        @Test
        @DisplayName("throws when booking not in RESERVED state")
        void confirmBooking_notReserved() {
            CustomerContext.setUserId(50L);
            Booking confirmed = booking(1L, 50L, Booking.Status.CONFIRMED, null);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(confirmed));

            var svc = service();
            assertThatThrownBy(() -> svc.confirmBooking(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in RESERVED");
        }

        @Test
        @DisplayName("throws when booking not found or not owned")
        void confirmBooking_forbidden() {
            CustomerContext.setUserId(50L);
            Booking reserved = booking(1L, 99L, Booking.Status.RESERVED, null);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reserved));

            var svc = service();
            assertThatThrownBy(() -> svc.confirmBooking(1L))
                    .isInstanceOf(ForbiddenBookingAccessException.class);
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("cancels RESERVED booking and releases seats")
        void cancelBooking_reserved() {
            CustomerContext.setUserId(50L);
            Booking reserved = booking(1L, 50L, Booking.Status.RESERVED, Instant.now().plusSeconds(300));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reserved));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            var svc = service();
            svc.cancelBooking(1L);

            verify(theatreSeatClient).releaseSeats(1L, 1L);
            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Booking.Status.CANCELLED);
        }

        @Test
        @DisplayName("idempotent when already CANCELLED")
        void cancelBooking_alreadyCancelled() {
            CustomerContext.setUserId(50L);
            Booking cancelled = booking(1L, 50L, Booking.Status.CANCELLED, null);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(cancelled));

            var svc = service();
            svc.cancelBooking(1L);

            // Does not call releaseSeats or save when already CANCELLED
        }

        @Test
        @DisplayName("throws when not owned by current user")
        void cancelBooking_forbidden() {
            CustomerContext.setUserId(50L);
            Booking b = booking(1L, 99L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));

            var svc = service();
            assertThatThrownBy(() -> svc.cancelBooking(1L))
                    .isInstanceOf(ForbiddenBookingAccessException.class);
        }
    }

    @Nested
    @DisplayName("bulkCancelBookings")
    class BulkCancel {

        @Test
        @DisplayName("cancels multiple and returns cancelledIds and failedIds")
        void bulkCancel_ok() {
            CustomerContext.setUserId(50L);
            Booking b1 = booking(1L, 50L, Booking.Status.RESERVED, null);
            Booking b2 = booking(2L, 50L, Booking.Status.CONFIRMED, null);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(b1));
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(b2));
            when(bookingRepository.findById(3L)).thenReturn(Optional.empty());
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            var svc = service();
            BulkCancelResponse resp = svc.bulkCancelBookings(new BulkCancelRequest().bookingIds(List.of(1L, 2L, 3L)));

            assertThat(resp.getCancelledIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(resp.getFailedIds()).containsExactly(3L);
            assertThat(resp.getErrors()).containsKey("3");
        }

        @Test
        @DisplayName("records failed when booking not owned by user")
        void bulkCancel_partialForbidden() {
            CustomerContext.setUserId(50L);
            Booking own = booking(1L, 50L, Booking.Status.RESERVED, null);
            Booking other = booking(2L, 99L, Booking.Status.RESERVED, null);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(own));
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(other));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            var svc = service();
            BulkCancelResponse resp = svc.bulkCancelBookings(new BulkCancelRequest().bookingIds(List.of(1L, 2L)));

            assertThat(resp.getCancelledIds()).containsExactly(1L);
            assertThat(resp.getFailedIds()).containsExactly(2L);
        }
    }
}

