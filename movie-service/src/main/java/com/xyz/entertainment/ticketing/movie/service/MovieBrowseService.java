package com.xyz.entertainment.ticketing.movie.service;

import com.xyz.entertainment.ticketing.movie.api.model.MovieDetails;
import com.xyz.entertainment.ticketing.movie.api.model.MovieSummary;
import com.xyz.entertainment.ticketing.movie.api.model.ShowResponse;
import com.xyz.entertainment.ticketing.movie.domain.Movie;
import com.xyz.entertainment.ticketing.movie.domain.Show;
import com.xyz.entertainment.ticketing.movie.repository.MovieRepository;
import com.xyz.entertainment.ticketing.movie.repository.ShowRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieBrowseService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public List<MovieSummary> listMovies(String city, String language, String genre) {
        List<Movie> movies = movieRepository.findAll();

        if (city != null && !city.isBlank()) {
            // Restrict movies to those that have at least one show in the given city
            Set<Long> movieIdsWithCity = showRepository.findAll().stream()
                    .filter(s -> city.equalsIgnoreCase(s.getCity()))
                    .map(Show::getMovieId)
                    .collect(Collectors.toSet());
            movies = movies.stream()
                    .filter(m -> movieIdsWithCity.contains(m.getId()))
                    .collect(Collectors.toList());
        }

        String langFilter = language != null ? language.trim().toLowerCase() : null;
        String genreFilter = genre != null ? genre.trim().toLowerCase() : null;

        return movies.stream()
                .filter(m -> m.isActive())
                .filter(m -> langFilter == null || (m.getLanguage() != null
                        && m.getLanguage().trim().toLowerCase().equals(langFilter)))
                .filter(m -> genreFilter == null || (m.getGenre() != null
                        && m.getGenre().trim().toLowerCase().equals(genreFilter)))
                .map(MovieBrowseService::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MovieDetails getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found"));
        return toDetails(movie);
    }

    /**
     * Browse theatres currently running the given movie in a town, with show timings for a chosen date.
     * Returns one entry per show: each includes theatreId, city, and startTime (show timing).
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> listMovieShows(Long movieId, String city, LocalDate date) {
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException("Movie not found");
        }

        List<Show> shows;
        if (date != null) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            shows = showRepository.findByMovieIdAndStartTimeBetween(movieId, dayStart, dayEnd);
        } else {
            shows = showRepository.findByMovieId(movieId);
        }

        if (city != null && !city.isBlank()) {
            String cityFilter = city.trim().toLowerCase();
            shows = shows.stream()
                    .filter(s -> s.getCity() != null
                            && s.getCity().trim().toLowerCase().equals(cityFilter))
                    .collect(Collectors.toList());
        }

        return shows.stream()
                .map(MovieBrowseService::toShowResponse)
                .collect(Collectors.toList());
    }

    private static MovieSummary toSummary(Movie movie) {
        return new MovieSummary()
                .id(movie.getId())
                .title(movie.getTitle())
                .language(movie.getLanguage())
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes());
    }

    private static MovieDetails toDetails(Movie movie) {
        return new MovieDetails()
                .id(movie.getId())
                .title(movie.getTitle())
                .language(movie.getLanguage())
                .genre(movie.getGenre())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes());
    }

    private static ShowResponse toShowResponse(Show show) {
        return new ShowResponse()
                .id(show.getId())
                .movieId(show.getMovieId())
                .theatreId(show.getTheatreId())
                .tenantId(show.getTenantId())
                .city(show.getCity())
                .language(show.getLanguage())
                .startTime(show.getStartTime().atOffset(ZoneOffset.UTC))
                .basePrice(show.getBasePrice().doubleValue());
    }
}

