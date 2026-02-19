package com.xyz.entertainment.ticketing.theatre.repository;

import com.xyz.entertainment.ticketing.theatre.domain.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScreenIdOrderByRowLabelAscSeatNumberAsc(Long screenId);

    List<Seat> findByScreenIdAndLabelIn(Long screenId, List<String> labels);
}
