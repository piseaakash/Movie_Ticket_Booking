package com.xyz.entertainment.ticketing.theatre.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatInventoryServiceTest {

    @Mock
    private ShowScreenRepository showScreenRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private ScreenRepository screenRepository;
    @Mock
    private TheatreRepository theatreRepository;
    @Mock
    private SeatRepository seatRepository;

    private static final Long TENANT_ID = 10L;
    private static final Long SHOW_ID = 100L;
    private static final Long SCREEN_ID = 1L;
    private static final Long THEATRE_ID = 5L;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void setContext(String... roles) {
        TenantContext.set(1L, TENANT_ID, List.of(roles));
    }

    private SeatInventoryService service() {
        return new SeatInventoryService(showScreenRepository, showSeatRepository, screenRepository,
                theatreRepository, seatRepository);
    }

    private Screen screen(Long id, Long theatreId) {
        return Screen.builder().id(id).theatreId(theatreId).name("S1").displayOrder(0)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private Theatre theatre(Long id, Long tenantId) {
        return Theatre.builder().id(id).tenantId(tenantId).name("T1")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private Seat seat(Long id, String label) {
        return Seat.builder().id(id).screenId(SCREEN_ID).rowLabel("A").seatNumber(1).label(label).build();
    }

    @Nested
    @DisplayName("registerShowToScreen")
    class RegisterShow {

        @Test
        @DisplayName("registers show and creates ShowSeat rows when partner and tenant match")
        void registerShow_ok() {
            setContext("PARTNER_ADMIN");
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(SCREEN_ID))
                    .thenReturn(List.of(seat(1L, "A1"), seat(2L, "A2")));

            RegisterShowRequest req = new RegisterShowRequest().screenId(SCREEN_ID);
            ShowScreenResponse resp = service().registerShowToScreen(SHOW_ID, req);

            assertThat(resp.getShowId()).isEqualTo(SHOW_ID);
            assertThat(resp.getScreenId()).isEqualTo(SCREEN_ID);
            verify(showScreenRepository).save(any(ShowScreen.class));
            verify(showSeatRepository, org.mockito.Mockito.times(2)).save(any(ShowSeat.class));
        }

        @Test
        @DisplayName("throws when show already registered")
        void registerShow_alreadyRegistered() {
            setContext("PARTNER_ADMIN");
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.of(
                    ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));

            assertThatThrownBy(() -> service().registerShowToScreen(SHOW_ID, new RegisterShowRequest().screenId(SCREEN_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
            verify(showScreenRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ForbiddenException when no partner role")
        void registerShow_forbidden() {
            setContext("CUSTOMER");
            assertThatThrownBy(() -> service().registerShowToScreen(SHOW_ID, new RegisterShowRequest().screenId(SCREEN_ID)))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws ScreenNotFoundException when screen not found")
        void registerShow_screenNotFound() {
            setContext("PARTNER_ADMIN");
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().registerShowToScreen(SHOW_ID, new RegisterShowRequest().screenId(SCREEN_ID)))
                    .isInstanceOf(ScreenNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void registerShow_tenantMismatch() {
            setContext("PARTNER_ADMIN");
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, 999L)));

            assertThatThrownBy(() -> service().registerShowToScreen(SHOW_ID, new RegisterShowRequest().screenId(SCREEN_ID)))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }
    }

    @Nested
    @DisplayName("getShowScreen")
    class GetShowScreen {

        @Test
        @DisplayName("returns show screen when registered")
        void getShowScreen_ok() {
            when(showScreenRepository.findByShowId(SHOW_ID))
                    .thenReturn(Optional.of(ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));

            ShowScreenResponse resp = service().getShowScreen(SHOW_ID);
            assertThat(resp.getShowId()).isEqualTo(SHOW_ID);
            assertThat(resp.getScreenId()).isEqualTo(SCREEN_ID);
        }

        @Test
        @DisplayName("throws ShowNotRegisteredException when show not registered")
        void getShowScreen_notRegistered() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().getShowScreen(SHOW_ID))
                    .isInstanceOf(ShowNotRegisteredException.class)
                    .hasMessageContaining("Show not registered");
        }
    }

    @Nested
    @DisplayName("getShowSeatAvailability")
    class GetAvailability {

        @Test
        @DisplayName("returns availability list when show registered")
        void getAvailability_ok() {
            when(showScreenRepository.findByShowId(SHOW_ID))
                    .thenReturn(Optional.of(ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));
            when(showSeatRepository.findByShowId(SHOW_ID)).thenReturn(List.of(
                    ShowSeat.builder().showId(SHOW_ID).seatId(1L).status(ShowSeat.Status.AVAILABLE).build(),
                    ShowSeat.builder().showId(SHOW_ID).seatId(2L).status(ShowSeat.Status.BOOKED).build()));
            when(seatRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(seat(1L, "A1"), seat(2L, "A2")));

            List<SeatAvailabilityResponse> list = service().getShowSeatAvailability(SHOW_ID);
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getStatus()).isEqualTo(SeatAvailabilityResponse.StatusEnum.AVAILABLE);
            assertThat(list.get(1).getStatus()).isEqualTo(SeatAvailabilityResponse.StatusEnum.BOOKED);
            verify(showSeatRepository).releaseExpiredLocks(any(Instant.class));
        }

        @Test
        @DisplayName("throws ShowNotRegisteredException when show not registered")
        void getAvailability_notRegistered() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().getShowSeatAvailability(SHOW_ID))
                    .isInstanceOf(ShowNotRegisteredException.class);
        }
    }

    @Nested
    @DisplayName("lockSeats")
    class LockSeats {

        @Test
        @DisplayName("locks seats when all available")
        void lockSeats_ok() {
            when(showScreenRepository.findByShowId(SHOW_ID))
                    .thenReturn(Optional.of(ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));
            when(seatRepository.findByScreenIdAndLabelIn(eq(SCREEN_ID), eq(List.of("A1", "A2"))))
                    .thenReturn(List.of(seat(1L, "A1"), seat(2L, "A2")));
            when(showSeatRepository.lockAvailableSeats(eq(SHOW_ID), eq(List.of(1L, 2L)), eq(50L), any(Instant.class)))
                    .thenReturn(2);

            LockSeatsRequest req = new LockSeatsRequest().seatLabels(List.of("A1", "A2")).bookingId(50L);
            service().lockSeats(SHOW_ID, req);

            verify(showSeatRepository).lockAvailableSeats(eq(SHOW_ID), eq(List.of(1L, 2L)), eq(50L), any(Instant.class));
        }

        @Test
        @DisplayName("throws ShowNotRegisteredException when show not registered")
        void lockSeats_notRegistered() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());

            LockSeatsRequest req = new LockSeatsRequest().seatLabels(List.of("A1")).bookingId(50L);
            assertThatThrownBy(() -> service().lockSeats(SHOW_ID, req))
                    .isInstanceOf(ShowNotRegisteredException.class);
        }

        @Test
        @DisplayName("throws SeatsNotAvailableException when seat labels not found")
        void lockSeats_labelsNotFound() {
            when(showScreenRepository.findByShowId(SHOW_ID))
                    .thenReturn(Optional.of(ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));
            when(seatRepository.findByScreenIdAndLabelIn(eq(SCREEN_ID), eq(List.of("A1", "X99"))))
                    .thenReturn(List.of(seat(1L, "A1"))); // only one found

            LockSeatsRequest req = new LockSeatsRequest().seatLabels(List.of("A1", "X99")).bookingId(50L);
            assertThatThrownBy(() -> service().lockSeats(SHOW_ID, req))
                    .isInstanceOf(SeatsNotAvailableException.class)
                    .hasMessageContaining("not found or duplicate");
        }

        @Test
        @DisplayName("throws SeatsNotAvailableException when lock update returns fewer rows")
        void lockSeats_partialLock() {
            when(showScreenRepository.findByShowId(SHOW_ID))
                    .thenReturn(Optional.of(ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));
            when(seatRepository.findByScreenIdAndLabelIn(eq(SCREEN_ID), eq(List.of("A1", "A2"))))
                    .thenReturn(List.of(seat(1L, "A1"), seat(2L, "A2")));
            when(showSeatRepository.lockAvailableSeats(eq(SHOW_ID), eq(List.of(1L, 2L)), eq(50L), any(Instant.class)))
                    .thenReturn(1); // only 1 row updated

            LockSeatsRequest req = new LockSeatsRequest().seatLabels(List.of("A1", "A2")).bookingId(50L);
            assertThatThrownBy(() -> service().lockSeats(SHOW_ID, req))
                    .isInstanceOf(SeatsNotAvailableException.class)
                    .hasMessageContaining("not available for lock");
        }
    }

    @Nested
    @DisplayName("confirmSeatLock")
    class ConfirmLock {

        @Test
        @DisplayName("confirms when show registered and seats locked")
        void confirm_ok() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.of(
                    ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));
            when(showSeatRepository.confirmLockedSeats(SHOW_ID, 50L)).thenReturn(2);

            ConfirmLockRequest req = new ConfirmLockRequest().bookingId(50L);
            service().confirmSeatLock(SHOW_ID, req);

            verify(showSeatRepository).confirmLockedSeats(SHOW_ID, 50L);
        }

        @Test
        @DisplayName("throws ShowNotRegisteredException when show not registered")
        void confirm_notRegistered() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().confirmSeatLock(SHOW_ID, new ConfirmLockRequest().bookingId(50L)))
                    .isInstanceOf(ShowNotRegisteredException.class);
        }

        @Test
        @DisplayName("throws when no locked seats for booking")
        void confirm_noLockedSeats() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.of(
                    ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));
            when(showSeatRepository.confirmLockedSeats(SHOW_ID, 50L)).thenReturn(0);

            assertThatThrownBy(() -> service().confirmSeatLock(SHOW_ID, new ConfirmLockRequest().bookingId(50L)))
                    .isInstanceOf(ShowNotRegisteredException.class)
                    .hasMessageContaining("No locked seats");
        }
    }

    @Nested
    @DisplayName("releaseSeatLock")
    class ReleaseLock {

        @Test
        @DisplayName("releases seats when show registered")
        void release_ok() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.of(
                    ShowScreen.builder().showId(SHOW_ID).screenId(SCREEN_ID).build()));

            ReleaseSeatsRequest req = new ReleaseSeatsRequest().bookingId(50L);
            service().releaseSeatLock(SHOW_ID, req);

            verify(showSeatRepository).releaseSeatsByBooking(SHOW_ID, 50L);
        }

        @Test
        @DisplayName("throws ShowNotRegisteredException when show not registered")
        void release_notRegistered() {
            when(showScreenRepository.findByShowId(SHOW_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().releaseSeatLock(SHOW_ID, new ReleaseSeatsRequest().bookingId(50L)))
                    .isInstanceOf(ShowNotRegisteredException.class);
        }
    }
}
