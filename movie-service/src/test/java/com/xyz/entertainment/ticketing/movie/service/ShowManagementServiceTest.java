package com.xyz.entertainment.ticketing.movie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.movie.api.model.CreateShowRequest;
import com.xyz.entertainment.ticketing.movie.api.model.ShowResponse;
import com.xyz.entertainment.ticketing.movie.api.model.UpdateShowRequest;
import com.xyz.entertainment.ticketing.movie.domain.Show;
import com.xyz.entertainment.ticketing.movie.repository.MovieRepository;
import com.xyz.entertainment.ticketing.movie.repository.ShowRepository;
import com.xyz.entertainment.ticketing.movie.security.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowManagementServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private MovieRepository movieRepository;

    private ShowManagementService service() {
        return new ShowManagementService(showRepository, movieRepository);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("createShow")
    class CreateShow {

        @Test
        @DisplayName("creates show when tenant and partner role")
        void success() {
            TenantContext.set(1L, 10L, List.of("THEATRE_MANAGER"));
            when(movieRepository.existsById(1L)).thenReturn(true);
            CreateShowRequest req = new CreateShowRequest(1L, 2L, "Pune", OffsetDateTime.now(), 150.0);
            req.setLanguage("English");
            Show saved = Show.builder()
                    .id(100L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city("Pune")
                    .language("English")
                    .startTime(req.getStartTime().toInstant())
                    .basePrice(BigDecimal.valueOf(150.0))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.save(any(Show.class))).thenReturn(saved);

            ShowResponse resp = service().createShow(req);

            assertThat(resp.getId()).isEqualTo(100L);
            assertThat(resp.getMovieId()).isEqualTo(1L);
            assertThat(resp.getTheatreId()).isEqualTo(2L);
            assertThat(resp.getTenantId()).isEqualTo(10L);
            assertThat(resp.getCity()).isEqualTo("Pune");
            assertThat(resp.getBasePrice()).isEqualTo(150.0);
            verify(movieRepository).existsById(1L);
            verify(showRepository).save(any(Show.class));
        }

        @Test
        @DisplayName("throws MovieNotFoundException when movie does not exist")
        void movieNotFound() {
            TenantContext.set(1L, 10L, List.of("PARTNER_ADMIN"));
            when(movieRepository.existsById(1L)).thenReturn(false);
            CreateShowRequest req = new CreateShowRequest(1L, 2L, "Pune", OffsetDateTime.now(), 100.0);

            assertThatThrownBy(() -> service().createShow(req))
                    .isInstanceOf(MovieNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant context missing")
        void noTenant() {
            TenantContext.set(1L, null, List.of("THEATRE_MANAGER"));
            CreateShowRequest req = new CreateShowRequest(1L, 2L, "Pune", OffsetDateTime.now(), 100.0);

            assertThatThrownBy(() -> service().createShow(req))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant context required");
        }

        @Test
        @DisplayName("throws ForbiddenException when no partner role")
        void noPartnerRole() {
            TenantContext.set(1L, 10L, List.of("CUSTOMER"));
            CreateShowRequest req = new CreateShowRequest(1L, 2L, "Pune", OffsetDateTime.now(), 100.0);

            assertThatThrownBy(() -> service().createShow(req))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Partner role required");
        }
    }

    @Nested
    @DisplayName("updateShow")
    class UpdateShow {

        @Test
        @DisplayName("updates show when same tenant")
        void success() {
            TenantContext.set(1L, 10L, List.of("THEATRE_MANAGER"));
            Show existing = Show.builder()
                    .id(50L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city("Pune")
                    .language("English")
                    .startTime(Instant.now())
                    .basePrice(BigDecimal.TEN)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findById(50L)).thenReturn(java.util.Optional.of(existing));
            OffsetDateTime newTime = OffsetDateTime.now().plusHours(2);
            UpdateShowRequest req = new UpdateShowRequest();
            req.setStartTime(newTime);
            req.setBasePrice(199.0);
            when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

            ShowResponse resp = service().updateShow(50L, req);

            assertThat(resp.getStartTime().toInstant()).isEqualTo(newTime.toInstant());
            assertThat(resp.getBasePrice()).isEqualTo(199.0);
            verify(showRepository).save(any(Show.class));
        }

        @Test
        @DisplayName("throws ShowNotFoundException when show missing")
        void notFound() {
            TenantContext.set(1L, 10L, List.of("THEATRE_MANAGER"));
            when(showRepository.findById(999L)).thenReturn(java.util.Optional.empty());
            UpdateShowRequest req = new UpdateShowRequest();

            assertThatThrownBy(() -> service().updateShow(999L, req))
                    .isInstanceOf(ShowNotFoundException.class)
                    .hasMessageContaining("Show not found");
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void tenantMismatch() {
            TenantContext.set(1L, 99L, List.of("THEATRE_MANAGER"));
            Show existing = Show.builder()
                    .id(50L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city("Pune")
                    .startTime(Instant.now())
                    .basePrice(BigDecimal.TEN)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findById(50L)).thenReturn(java.util.Optional.of(existing));
            UpdateShowRequest req = new UpdateShowRequest();

            assertThatThrownBy(() -> service().updateShow(50L, req))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
        }

        @Test
        @DisplayName("updates only startTime when basePrice not provided")
        void partialUpdateStartTime() {
            TenantContext.set(1L, 10L, List.of("PARTNER_ADMIN"));
            Instant existingTime = Instant.now();
            Show existing = Show.builder()
                    .id(50L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city("Pune")
                    .startTime(existingTime)
                    .basePrice(BigDecimal.valueOf(120.0))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findById(50L)).thenReturn(java.util.Optional.of(existing));
            OffsetDateTime newTime = OffsetDateTime.of(2025, 6, 15, 14, 0, 0, 0, ZoneOffset.UTC);
            UpdateShowRequest req = new UpdateShowRequest();
            req.setStartTime(newTime);
            when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

            ShowResponse resp = service().updateShow(50L, req);

            assertThat(resp.getStartTime().toInstant()).isEqualTo(newTime.toInstant());
            assertThat(resp.getBasePrice()).isEqualTo(120.0);
        }
    }

    @Nested
    @DisplayName("deleteShow")
    class DeleteShow {

        @Test
        @DisplayName("deletes show when same tenant")
        void success() {
            TenantContext.set(1L, 10L, List.of("THEATRE_MANAGER"));
            Show show = Show.builder()
                    .id(50L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city("Pune")
                    .startTime(Instant.now())
                    .basePrice(BigDecimal.TEN)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findById(50L)).thenReturn(java.util.Optional.of(show));

            service().deleteShow(50L);

            verify(showRepository).deleteById(50L);
        }

        @Test
        @DisplayName("throws ShowNotFoundException when show missing")
        void notFound() {
            TenantContext.set(1L, 10L, List.of("THEATRE_MANAGER"));
            when(showRepository.findById(999L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service().deleteShow(999L))
                    .isInstanceOf(ShowNotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when tenant mismatch")
        void tenantMismatch() {
            TenantContext.set(1L, 99L, List.of("THEATRE_MANAGER"));
            Show show = Show.builder()
                    .id(50L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city("Pune")
                    .startTime(Instant.now())
                    .basePrice(BigDecimal.TEN)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findById(50L)).thenReturn(java.util.Optional.of(show));

            assertThatThrownBy(() -> service().deleteShow(50L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Tenant mismatch");
            verify(showRepository).findById(50L);
            // deleteById must not be called
        }
    }
}
