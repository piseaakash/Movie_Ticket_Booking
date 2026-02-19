package com.xyz.entertainment.ticketing.movie.repository;

import com.xyz.entertainment.ticketing.movie.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}

