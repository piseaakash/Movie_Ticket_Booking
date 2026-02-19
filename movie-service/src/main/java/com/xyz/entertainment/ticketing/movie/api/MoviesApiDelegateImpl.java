package com.xyz.entertainment.ticketing.movie.api;

import com.xyz.entertainment.ticketing.movie.api.model.MovieDetails;
import com.xyz.entertainment.ticketing.movie.api.model.MovieSummary;
import com.xyz.entertainment.ticketing.movie.api.model.ShowResponse;
import com.xyz.entertainment.ticketing.movie.service.MovieBrowseService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class MoviesApiDelegateImpl implements MoviesApiDelegate {

    private final MovieBrowseService movieBrowseService;

    @Override
    public ResponseEntity<List<MovieSummary>> listMovies(String city, String language, String genre) {
        List<MovieSummary> movies = movieBrowseService.listMovies(city, language, genre);
        return ResponseEntity.ok(movies);
    }

    @Override
    public ResponseEntity<MovieDetails> getMovie(Long id) {
        MovieDetails details = movieBrowseService.getMovie(id);
        return ResponseEntity.ok(details);
    }

    @Override
    public ResponseEntity<List<ShowResponse>> listMovieShows(Long id, String city, LocalDate date) {
        List<ShowResponse> shows = movieBrowseService.listMovieShows(id, city, date);
        return ResponseEntity.ok(shows);
    }
}

