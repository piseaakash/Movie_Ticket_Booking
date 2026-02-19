package com.xyz.entertainment.ticketing.movie.repository;

import com.xyz.entertainment.ticketing.movie.domain.Show;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);

    /**
     * Shows for a movie whose start time falls within [startInclusive, endExclusive).
     * Used for date-filtered browse (e.g. start/end of chosen date in UTC).
     */
    List<Show> findByMovieIdAndStartTimeBetween(Long movieId, Instant startInclusive, Instant endExclusive);
}

