package com.xyz.entertainment.ticketing.theatre.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateScreenRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.ScreenResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.UpdateScreenRequest;
import com.xyz.entertainment.ticketing.theatre.domain.Screen;
import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
import com.xyz.entertainment.ticketing.theatre.repository.ScreenRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScreenServiceTest {

    @Mock
    private ScreenRepository screenRepository;
    @Mock
    private TheatreRepository theatreRepository;

    private static final Long TENANT_ID = 10L;
    private static final Long THEATRE_ID = 1L;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void setContext(String... roles) {
        TenantContext.set(1L, TENANT_ID, List.of(roles));
    }

    private ScreenService service() {
        return new ScreenService(screenRepository, theatreRepository);
    }

    private Theatre theatre(Long id, Long tenantId) {
        return Theatre.builder()
                .id(id)
                .tenantId(tenantId)
                .name("Theatre")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Screen screen(Long id, Long theatreId) {
        return Screen.builder()
                .id(id)
                .theatreId(theatreId)
                .name("Screen 1")
                .displayOrder(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("createScreen")
    class CreateScreen {

        @Test
        @DisplayName("creates screen when partner role and tenant match")
        void createScreen_ok() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> {
                Screen s = inv.getArgument(0);
                return Screen.builder().id(5L).theatreId(s.getTheatreId()).name(s.getName())
                        .displayOrder(s.getDisplayOrder()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build();
            });

            CreateScreenRequest req = new CreateScreenRequest().name("Screen A").displayOrder(1);
            ScreenResponse resp = service().createScreen(THEATRE_ID, req);

            assertThat(resp.getId()).isEqualTo(5L);
            assertThat(resp.getTheatreId()).isEqualTo(THEATRE_ID);
            assertThat(resp.getName()).isEqualTo("Screen A");
            assertThat(resp.getDisplayOrder()).isEqualTo(1);
            ArgumentCaptor<Screen> captor = ArgumentCaptor.forClass(Screen.class);
            verify(screenRepository).save(captor.capture());
            assertThat(captor.getValue().getTheatreId()).isEqualTo(THEATRE_ID);
        }

        @Test
        @DisplayName("throws ForbiddenException when no partner role")
        void createScreen_forbidden() {
            setContext("CUSTOMER");
            assertThatThrownBy(() -> service().createScreen(THEATRE_ID, new CreateScreenRequest().name("X")))
                    .isInstanceOf(ForbiddenException.class);
            verify(screenRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TheatreNotFoundException when theatre not found")
        void createScreen_theatreNotFound() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().createScreen(THEATRE_ID, new CreateScreenRequest().name("X")))
                    .isInstanceOf(TheatreNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void createScreen_tenantMismatch() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, 999L)));
            assertThatThrownBy(() -> service().createScreen(THEATRE_ID, new CreateScreenRequest().name("X")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }
    }

    @Nested
    @DisplayName("listScreens")
    class ListScreens {

        @Test
        @DisplayName("returns screens for theatre when tenant matches")
        void listScreens_ok() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(screenRepository.findByTheatreIdOrderByDisplayOrderAsc(THEATRE_ID))
                    .thenReturn(List.of(screen(1L, THEATRE_ID), screen(2L, THEATRE_ID)));

            List<ScreenResponse> list = service().listScreens(THEATRE_ID);
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("throws when tenant context missing")
        void listScreens_noTenant() {
            TenantContext.set(1L, null, List.of("PARTNER_ADMIN"));
            assertThatThrownBy(() -> service().listScreens(THEATRE_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant context");
        }
    }

    @Nested
    @DisplayName("getScreen")
    class GetScreen {

        @Test
        @DisplayName("returns screen when theatre and screen belong to tenant")
        void getScreen_ok() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(screenRepository.findById(2L)).thenReturn(Optional.of(screen(2L, THEATRE_ID)));

            ScreenResponse resp = service().getScreen(THEATRE_ID, 2L);
            assertThat(resp.getId()).isEqualTo(2L);
            assertThat(resp.getTheatreId()).isEqualTo(THEATRE_ID);
        }

        @Test
        @DisplayName("throws ScreenNotFoundException when screen not in theatre")
        void getScreen_screenWrongTheatre() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(screenRepository.findById(2L)).thenReturn(Optional.of(screen(2L, 999L))); // different theatre

            assertThatThrownBy(() -> service().getScreen(THEATRE_ID, 2L))
                    .isInstanceOf(ScreenNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateScreen")
    class UpdateScreen {

        @Test
        @DisplayName("updates screen when partner role and tenant match")
        void updateScreen_ok() {
            setContext("THEATRE_MANAGER");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            Screen s = screen(3L, THEATRE_ID);
            when(screenRepository.findById(3L)).thenReturn(Optional.of(s));

            UpdateScreenRequest req = new UpdateScreenRequest().name("Updated").displayOrder(2);
            ScreenResponse resp = service().updateScreen(THEATRE_ID, 3L, req);

            assertThat(resp.getName()).isEqualTo("Updated");
            assertThat(resp.getDisplayOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("deleteScreen")
    class DeleteScreen {

        @Test
        @DisplayName("deletes screen when partner role and tenant match")
        void deleteScreen_ok() {
            setContext("PARTNER_ADMIN");
            when(theatreRepository.findById(THEATRE_ID)).thenReturn(Optional.of(theatre(THEATRE_ID, TENANT_ID)));
            when(screenRepository.findById(4L)).thenReturn(Optional.of(screen(4L, THEATRE_ID)));

            service().deleteScreen(THEATRE_ID, 4L);
            verify(screenRepository).deleteById(4L);
        }
    }
}
