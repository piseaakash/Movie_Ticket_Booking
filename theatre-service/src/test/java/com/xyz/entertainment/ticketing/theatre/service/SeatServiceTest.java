package com.xyz.entertainment.ticketing.theatre.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;
    @Mock
    private ScreenRepository screenRepository;
    @Mock
    private TheatreRepository theatreRepository;

    private static final Long TENANT_ID = 10L;
    private static final Long SCREEN_ID = 1L;
    private static final Long THEATRE_ID = 5L;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void setContext(String... roles) {
        TenantContext.set(1L, TENANT_ID, List.of(roles));
    }

    private SeatService service() {
        return new SeatService(seatRepository, screenRepository, theatreRepository);
    }

    private Screen screen(Long id, Long theatreId) {
        return Screen.builder().id(id).theatreId(theatreId).name("S1").displayOrder(0)
                .createdAt(java.time.Instant.now()).updatedAt(java.time.Instant.now()).build();
    }

    private Theatre theatre(Long id, Long tenantId) {
        return Theatre.builder().id(id).tenantId(tenantId).name("T1")
                .createdAt(java.time.Instant.now()).updatedAt(java.time.Instant.now()).build();
    }

    private Seat seat(Long id, Long screenId, String label) {
        return Seat.builder().id(id).screenId(screenId).rowLabel("A").seatNumber(1).label(label).build();
    }

    @Nested
    @DisplayName("createSeat")
    class CreateSeat {

        @Test
        @DisplayName("creates seat when partner role and screen belongs to tenant")
        void createSeat_ok() {
            setContext("PARTNER_ADMIN");
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> {
                Seat s = inv.getArgument(0);
                return Seat.builder().id(10L).screenId(s.getScreenId()).rowLabel(s.getRowLabel())
                        .seatNumber(s.getSeatNumber()).label(s.getLabel()).build();
            });

            CreateSeatRequest req = new CreateSeatRequest().rowLabel("A").seatNumber(1).label("A1");
            SeatResponse resp = service().createSeat(SCREEN_ID, req);

            assertThat(resp.getId()).isEqualTo(10L);
            assertThat(resp.getScreenId()).isEqualTo(SCREEN_ID);
            assertThat(resp.getLabel()).isEqualTo("A1");
            ArgumentCaptor<Seat> captor = ArgumentCaptor.forClass(Seat.class);
            verify(seatRepository).save(captor.capture());
            assertThat(captor.getValue().getLabel()).isEqualTo("A1");
        }

        @Test
        @DisplayName("throws ForbiddenException when no partner role")
        void createSeat_forbidden() {
            setContext("CUSTOMER");
            assertThatThrownBy(() -> service().createSeat(SCREEN_ID,
                    new CreateSeatRequest().rowLabel("A").seatNumber(1).label("A1")))
                    .isInstanceOf(ForbiddenException.class);
            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ScreenNotFoundException when screen not found")
        void createSeat_screenNotFound() {
            setContext("PARTNER_ADMIN");
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().createSeat(SCREEN_ID,
                    new CreateSeatRequest().rowLabel("A").seatNumber(1).label("A1")))
                    .isInstanceOf(ScreenNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void createSeat_tenantMismatch() {
            setContext("PARTNER_ADMIN");
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, 999L)));
            assertThatThrownBy(() -> service().createSeat(SCREEN_ID,
                    new CreateSeatRequest().rowLabel("A").seatNumber(1).label("A1")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }
    }

    @Nested
    @DisplayName("listSeats")
    class ListSeats {

        @Test
        @DisplayName("returns seats for screen when tenant matches")
        void listSeats_ok() {
            setContext("PARTNER_ADMIN");
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(SCREEN_ID))
                    .thenReturn(List.of(seat(1L, SCREEN_ID, "A1"), seat(2L, SCREEN_ID, "A2")));

            List<SeatResponse> list = service().listSeats(SCREEN_ID);
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getLabel()).isEqualTo("A1");
        }
    }

    @Nested
    @DisplayName("deleteSeat")
    class DeleteSeat {

        @Test
        @DisplayName("deletes seat when partner role and seat belongs to screen")
        void deleteSeat_ok() {
            setContext("PARTNER_ADMIN");
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            Seat s = seat(1L, SCREEN_ID, "A1");
            when(seatRepository.findById(1L)).thenReturn(Optional.of(s));

            service().deleteSeat(SCREEN_ID, 1L);
            verify(seatRepository).delete(s);
        }

        @Test
        @DisplayName("throws SeatNotFoundException when seat not in screen")
        void deleteSeat_wrongScreen() {
            setContext("PARTNER_ADMIN");
            when(screenRepository.findById(SCREEN_ID)).thenReturn(Optional.of(screen(SCREEN_ID, THEATRE_ID)));
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(seatRepository.findById(1L)).thenReturn(Optional.of(seat(1L, 999L, "A1"))); // different screen

            assertThatThrownBy(() -> service().deleteSeat(SCREEN_ID, 1L))
                    .isInstanceOf(SeatNotFoundException.class);
        }
    }
}
