package com.xyz.entertainment.ticketing.booking.repository;

import com.xyz.entertainment.ticketing.booking.domain.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);
}

