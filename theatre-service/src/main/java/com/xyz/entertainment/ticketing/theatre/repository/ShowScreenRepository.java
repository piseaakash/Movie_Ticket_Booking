package com.xyz.entertainment.ticketing.theatre.repository;

import com.xyz.entertainment.ticketing.theatre.domain.ShowScreen;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowScreenRepository extends JpaRepository<ShowScreen, Long> {

    Optional<ShowScreen> findByShowId(Long showId);
}
