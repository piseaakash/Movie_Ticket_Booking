package com.xyz.entertainment.ticketing.theatre.repository;

import com.xyz.entertainment.ticketing.theatre.domain.Screen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findByTheatreIdOrderByDisplayOrderAsc(Long theatreId);
}
