package com.xyz.entertainment.ticketing.movie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.movie.api.model.MovieDetails;
import com.xyz.entertainment.ticketing.movie.api.model.MovieSummary;
import com.xyz.entertainment.ticketing.movie.api.model.ShowResponse;
import com.xyz.entertainment.ticketing.movie.domain.Movie;
import com.xyz.entertainment.ticketing.movie.domain.Show;
import com.xyz.entertainment.ticketing.movie.repository.MovieRepository;
import com.xyz.entertainment.ticketing.movie.repository.ShowRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieBrowseServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ShowRepository showRepository;

    private MovieBrowseService service() {
        return new MovieBrowseService(movieRepository, showRepository);
    }

    private Movie activeMovie(Long id, String title, String lang, String genre) {
        return Movie.builder()
                .id(id)
                .title(title)
                .language(lang)
                .genre(genre)
                .durationMinutes(120)
                .description("desc")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Show show(Long id, Long movieId, String city, String language, Instant startTime) {
        return Show.builder()
                .id(id)
                .movieId(movieId)
                .theatreId(1L)
                .tenantId(10L)
                .city(city)
                .language(language)
                .startTime(startTime)
                .basePrice(BigDecimal.TEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("listMovies")
    class ListMovies {

        @Test
        @DisplayName("returns all active movies when no filters")
        void listMovies_noFilters() {
            Movie m1 = activeMovie(1L, "A", "English", "Action");
            Movie m2 = activeMovie(2L, "B", "Hindi", "Drama");
            when(movieRepository.findAll()).thenReturn(List.of(m1, m2));

            var svc = service();
            List<MovieSummary> result = svc.listMovies(null, null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("filters by language and genre")
        void listMovies_languageAndGenre() {
            Movie m1 = activeMovie(1L, "A", "English", "Action");
            Movie m2 = activeMovie(2L, "B", "Hindi", "Drama");
            when(movieRepository.findAll()).thenReturn(List.of(m1, m2));

            var svc = service();
            List<MovieSummary> result = svc.listMovies(null, "hindi", "drama");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("filters by city via shows")
        void listMovies_cityFilter() {
            Movie m1 = activeMovie(1L, "A", "English", "Action");
            Movie m2 = activeMovie(2L, "B", "Hindi", "Drama");
            when(movieRepository.findAll()).thenReturn(List.of(m1, m2));

            Show s1 = show(1L, 1L, "Pune", "English", Instant.now());
            Show s2 = show(2L, 2L, "Mumbai", "Hindi", Instant.now());
            when(showRepository.findAll()).thenReturn(List.of(s1, s2));

            var svc = service();
            List<MovieSummary> result = svc.listMovies("Pune", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getMovie")
    class GetMovie {

        @Test
        @DisplayName("returns details when movie exists")
        void getMovie_ok() {
            Movie m = activeMovie(1L, "A", "English", "Action");
            when(movieRepository.findById(1L)).thenReturn(Optional.of(m));

            var svc = service();
            MovieDetails details = svc.getMovie(1L);

            assertThat(details.getId()).isEqualTo(1L);
            assertThat(details.getTitle()).isEqualTo("A");
        }

        @Test
        @DisplayName("throws MovieNotFoundException when movie missing")
        void getMovie_notFound() {
            when(movieRepository.findById(1L)).thenReturn(Optional.empty());

            var svc = service();
            assertThatThrownBy(() -> svc.getMovie(1L))
                    .isInstanceOf(MovieNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listMovieShows")
    class ListMovieShows {

        @Test
        @DisplayName("throws MovieNotFoundException when movie does not exist")
        void movieDoesNotExist() {
            when(movieRepository.existsById(1L)).thenReturn(false);

            var svc = service();
            assertThatThrownBy(() -> svc.listMovieShows(1L, null, null))
                    .isInstanceOf(MovieNotFoundException.class);
        }

        @Test
        @DisplayName("returns all shows for movie without filters")
        void listShows_noFilters() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Show s1 = show(1L, 1L, "Pune", "English", Instant.now());
            Show s2 = show(2L, 1L, "Mumbai", "Hindi", Instant.now());
            when(showRepository.findByMovieId(1L)).thenReturn(List.of(s1, s2));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("filters shows by city")
        void listShows_cityFilter() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Show s1 = show(1L, 1L, "Pune", "English", Instant.now());
            Show s2 = show(2L, 1L, "Mumbai", "Hindi", Instant.now());
            when(showRepository.findByMovieId(1L)).thenReturn(List.of(s1, s2));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, "Pune", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCity()).isEqualTo("Pune");
        }

        @Test
        @DisplayName("filters shows by date")
        void listShows_dateFilter() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Instant today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
            Show s1 = show(1L, 1L, "Pune", "English", today.plusSeconds(3600));
            when(showRepository.findByMovieIdAndStartTimeBetween(eq(1L), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(s1));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, null, LocalDate.now());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("filters by date uses correct UTC day range")
        void listShows_dateFilter_usesCorrectUtcDayRange() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            LocalDate chosen = LocalDate.of(2025, 3, 15);
            when(showRepository.findByMovieIdAndStartTimeBetween(eq(1L), any(Instant.class), any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            var svc = service();
            svc.listMovieShows(1L, null, chosen);

            ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(showRepository).findByMovieIdAndStartTimeBetween(
                    eq(1L), startCaptor.capture(), endCaptor.capture());
            assertThat(startCaptor.getValue()).isEqualTo(chosen.atStartOfDay(ZoneOffset.UTC).toInstant());
            assertThat(endCaptor.getValue()).isEqualTo(chosen.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        }

        @Test
        @DisplayName("filters by both date and city")
        void listShows_dateAndCityFilter() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Instant dayStart = LocalDate.of(2025, 4, 10).atStartOfDay(ZoneOffset.UTC).toInstant();
            Show s1 = show(1L, 1L, "Pune", "English", dayStart.plusSeconds(3600));
            Show s2 = show(2L, 1L, "Mumbai", "Hindi", dayStart.plusSeconds(7200));
            when(showRepository.findByMovieIdAndStartTimeBetween(eq(1L), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(s1, s2));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, "Pune", LocalDate.of(2025, 4, 10));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCity()).isEqualTo("Pune");
            assertThat(result.get(0).getTheatreId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("city filter is case-insensitive")
        void listShows_cityFilter_caseInsensitive() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Show s1 = show(1L, 1L, "Pune", "English", Instant.now());
            when(showRepository.findByMovieId(1L)).thenReturn(List.of(s1));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, "  PUNE  ", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCity()).isEqualTo("Pune");
        }

        @Test
        @DisplayName("blank city is ignored and all shows returned")
        void listShows_cityFilter_blankIgnored() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Show s1 = show(1L, 1L, "Pune", "English", Instant.now());
            Show s2 = show(2L, 1L, "Mumbai", "Hindi", Instant.now());
            when(showRepository.findByMovieId(1L)).thenReturn(List.of(s1, s2));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, "   ", null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("shows with null city are excluded when city filter is applied")
        void listShows_cityFilter_excludesNullCity() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Show withCity = show(1L, 1L, "Pune", "English", Instant.now());
            Show noCity = Show.builder()
                    .id(2L)
                    .movieId(1L)
                    .theatreId(2L)
                    .tenantId(10L)
                    .city(null)
                    .language("Hindi")
                    .startTime(Instant.now())
                    .basePrice(BigDecimal.TEN)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findByMovieId(1L)).thenReturn(List.of(withCity, noCity));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, "Pune", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("date filter returns empty when no shows on that date")
        void listShows_dateFilter_emptyWhenNoShowsOnDate() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            when(showRepository.findByMovieIdAndStartTimeBetween(eq(1L), any(Instant.class), any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, null, LocalDate.of(2025, 1, 1));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("response includes theatreId, startTime and city for browse scenario")
        void listShows_responseContainsTheatreAndTiming() {
            when(movieRepository.existsById(1L)).thenReturn(true);
            Instant startTime = LocalDate.of(2025, 5, 20).atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(5400);
            Show s1 = Show.builder()
                    .id(1L)
                    .movieId(1L)
                    .theatreId(42L)
                    .tenantId(10L)
                    .city("Pune")
                    .language("English")
                    .startTime(startTime)
                    .basePrice(BigDecimal.valueOf(199.50))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(showRepository.findByMovieIdAndStartTimeBetween(eq(1L), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(s1));

            var svc = service();
            List<ShowResponse> result = svc.listMovieShows(1L, "Pune", LocalDate.of(2025, 5, 20));

            assertThat(result).hasSize(1);
            ShowResponse resp = result.get(0);
            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getMovieId()).isEqualTo(1L);
            assertThat(resp.getTheatreId()).isEqualTo(42L);
            assertThat(resp.getCity()).isEqualTo("Pune");
            assertThat(resp.getStartTime()).isNotNull();
            assertThat(resp.getStartTime().toInstant()).isEqualTo(startTime);
            assertThat(resp.getBasePrice()).isEqualTo(199.50);
        }

        @Test
        @DisplayName("throws when movie does not exist even with date filter")
        void movieDoesNotExist_withDate() {
            when(movieRepository.existsById(1L)).thenReturn(false);

            var svc = service();
            assertThatThrownBy(() -> svc.listMovieShows(1L, "Pune", LocalDate.now()))
                    .isInstanceOf(MovieNotFoundException.class);
        }
    }
}

