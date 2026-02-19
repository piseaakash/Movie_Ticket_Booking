package com.xyz.entertainment.ticketing.theatre.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateTheatreRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.UpdateTheatreRequest;
import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
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
class TheatreServiceTest {

    @Mock
    private TheatreRepository theatreRepository;

    private TheatreService service;

    private static final Long TENANT_ID = 10L;
    private static final Long USER_ID = 1L;

    private void setContextWithRoles(String... roles) {
        TenantContext.set(USER_ID, TENANT_ID, List.of(roles));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private TheatreService newService() {
        if (service == null) {
            service = new TheatreService(theatreRepository);
        }
        return service;
    }

    @Nested
    @DisplayName("createTheatre")
    class CreateTheatre {

        @Test
        @DisplayName("creates theatre when caller has partner role")
        void createTheatre_withPartnerRole() {
            setContextWithRoles("PARTNER_ADMIN");
            CreateTheatreRequest req = new CreateTheatreRequest()
                    .name("My Theatre")
                    .addressLine1("Addr")
                    .city("City")
                    .state("State")
                    .postalCode("12345");

            when(theatreRepository.save(any(Theatre.class))).thenAnswer(inv -> {
                Theatre t = inv.getArgument(0);
                return Theatre.builder()
                        .id(5L)
                        .tenantId(t.getTenantId())
                        .name(t.getName())
                        .addressLine1(t.getAddressLine1())
                        .city(t.getCity())
                        .state(t.getState())
                        .postalCode(t.getPostalCode())
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .build();
            });

            TheatreService svc = newService();
            var resp = svc.createTheatre(req);

            assertThat(resp.getId()).isEqualTo(5L);
            assertThat(resp.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(resp.getName()).isEqualTo("My Theatre");

            ArgumentCaptor<Theatre> captor = ArgumentCaptor.forClass(Theatre.class);
            verify(theatreRepository).save(captor.capture());
            Theatre saved = captor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getName()).isEqualTo("My Theatre");
        }

        @Test
        @DisplayName("throws ForbiddenException when caller has no partner role")
        void createTheatre_withoutPartnerRole() {
            setContextWithRoles("CUSTOMER");
            CreateTheatreRequest req = new CreateTheatreRequest().name("X").city("C");

            TheatreService svc = newService();

            assertThatThrownBy(() -> svc.createTheatre(req))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Partner role required");

            verify(theatreRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listTheatres")
    class ListTheatres {

        @Test
        @DisplayName("returns theatres for current tenant")
        void listTheatres_scopedByTenant() {
            TenantContext.set(USER_ID, TENANT_ID, List.of("PARTNER_ADMIN"));

            Theatre t1 = Theatre.builder()
                    .id(1L).tenantId(TENANT_ID).name("A").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            Theatre t2 = Theatre.builder()
                    .id(2L).tenantId(TENANT_ID).name("B").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findByTenantId(eq(TENANT_ID))).thenReturn(List.of(t1, t2));

            TheatreService svc = newService();
            var list = svc.listTheatres();

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getTenantId()).isEqualTo(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("getTheatre")
    class GetTheatre {

        @Test
        @DisplayName("returns theatre when tenant matches")
        void getTheatre_ok() {
            TenantContext.set(USER_ID, TENANT_ID, List.of("PARTNER_ADMIN"));
            Theatre t = Theatre.builder()
                    .id(3L).tenantId(TENANT_ID).name("A").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findById(3L)).thenReturn(Optional.of(t));

            TheatreService svc = newService();
            var resp = svc.getTheatre(3L);

            assertThat(resp.getId()).isEqualTo(3L);
            assertThat(resp.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("throws TheatreNotFoundException when not found")
        void getTheatre_notFound() {
            TenantContext.set(USER_ID, TENANT_ID, List.of("PARTNER_ADMIN"));
            when(theatreRepository.findById(99L)).thenReturn(Optional.empty());

            TheatreService svc = newService();

            assertThatThrownBy(() -> svc.getTheatre(99L))
                    .isInstanceOf(TheatreNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatches")
        void getTheatre_tenantMismatch() {
            TenantContext.set(USER_ID, TENANT_ID, List.of("PARTNER_ADMIN"));
            Theatre t = Theatre.builder()
                    .id(3L).tenantId(999L).name("A").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findById(3L)).thenReturn(Optional.of(t));

            TheatreService svc = newService();

            assertThatThrownBy(() -> svc.getTheatre(3L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }
    }

    @Nested
    @DisplayName("updateTheatre")
    class UpdateTheatre {

        @Test
        @DisplayName("updates theatre when caller has partner role and tenant matches")
        void updateTheatre_ok() {
            setContextWithRoles("THEATRE_MANAGER");
            Theatre t = Theatre.builder()
                    .id(4L).tenantId(TENANT_ID).name("Old").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findById(4L)).thenReturn(Optional.of(t));

            UpdateTheatreRequest req = new UpdateTheatreRequest()
                    .name("New Name");

            TheatreService svc = newService();
            var resp = svc.updateTheatre(4L, req);

            assertThat(resp.getName()).isEqualTo("New Name");
            verify(theatreRepository).findById(4L);
        }

        @Test
        @DisplayName("throws ForbiddenException when caller has no partner role")
        void updateTheatre_withoutPartnerRole() {
            setContextWithRoles("CUSTOMER");
            TheatreService svc = newService();

            assertThatThrownBy(() -> svc.updateTheatre(4L, new UpdateTheatreRequest()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws TheatreNotFoundException when theatre not found")
        void updateTheatre_notFound() {
            setContextWithRoles("PARTNER_ADMIN");
            when(theatreRepository.findById(4L)).thenReturn(Optional.empty());

            TheatreService svc = newService();
            assertThatThrownBy(() -> svc.updateTheatre(4L, new UpdateTheatreRequest()))
                    .isInstanceOf(TheatreNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void updateTheatre_tenantMismatch() {
            setContextWithRoles("PARTNER_ADMIN");
            Theatre t = Theatre.builder()
                    .id(4L).tenantId(999L).name("Old").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findById(4L)).thenReturn(Optional.of(t));

            TheatreService svc = newService();
            assertThatThrownBy(() -> svc.updateTheatre(4L, new UpdateTheatreRequest()))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }
    }

    @Nested
    @DisplayName("deleteTheatre")
    class DeleteTheatre {

        @Test
        @DisplayName("deletes theatre when caller has partner role and tenant matches")
        void deleteTheatre_ok() {
            setContextWithRoles("PARTNER_ADMIN");
            Theatre t = Theatre.builder()
                    .id(7L).tenantId(TENANT_ID).name("Del").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findById(7L)).thenReturn(Optional.of(t));

            TheatreService svc = newService();
            svc.deleteTheatre(7L);

            verify(theatreRepository).delete(t);
        }

        @Test
        @DisplayName("throws ForbiddenException when caller has no partner role")
        void deleteTheatre_withoutPartnerRole() {
            setContextWithRoles("CUSTOMER");
            TheatreService svc = newService();

            assertThatThrownBy(() -> svc.deleteTheatre(7L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws TheatreNotFoundException when theatre not found")
        void deleteTheatre_notFound() {
            setContextWithRoles("PARTNER_ADMIN");
            when(theatreRepository.findById(7L)).thenReturn(Optional.empty());

            TheatreService svc = newService();
            assertThatThrownBy(() -> svc.deleteTheatre(7L))
                    .isInstanceOf(TheatreNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void deleteTheatre_tenantMismatch() {
            setContextWithRoles("PARTNER_ADMIN");
            Theatre t = Theatre.builder()
                    .id(7L).tenantId(999L).name("Del").createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
            when(theatreRepository.findById(7L)).thenReturn(Optional.of(t));

            TheatreService svc = newService();
            assertThatThrownBy(() -> svc.deleteTheatre(7L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }
    }
}

