package com.xyz.entertainment.ticketing.theatre.repository;

import com.xyz.entertainment.ticketing.theatre.domain.ShowSeat;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, ShowSeat.ShowSeatId> {

    List<ShowSeat> findByShowId(Long showId);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = 'LOCKED', s.lockedUntil = :lockedUntil, s.lockedByBookingId = :bookingId "
            + "WHERE s.showId = :showId AND s.seatId IN :seatIds AND s.status = 'AVAILABLE'")
    int lockAvailableSeats(
            @Param("showId") Long showId,
            @Param("seatIds") List<Long> seatIds,
            @Param("bookingId") Long bookingId,
            @Param("lockedUntil") Instant lockedUntil);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = 'AVAILABLE', s.lockedUntil = null, s.lockedByBookingId = null "
            + "WHERE s.showId = :showId AND s.lockedByBookingId = :bookingId AND s.status = 'LOCKED'")
    int releaseExpiredOrLockedByBooking(
            @Param("showId") Long showId,
            @Param("bookingId") Long bookingId);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = 'BOOKED', s.lockedUntil = null "
            + "WHERE s.showId = :showId AND s.lockedByBookingId = :bookingId AND s.status = 'LOCKED'")
    int confirmLockedSeats(@Param("showId") Long showId, @Param("bookingId") Long bookingId);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = 'AVAILABLE', s.lockedUntil = null, s.lockedByBookingId = null "
            + "WHERE s.showId = :showId AND s.lockedByBookingId = :bookingId")
    int releaseSeatsByBooking(@Param("showId") Long showId, @Param("bookingId") Long bookingId);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = 'AVAILABLE', s.lockedUntil = null, s.lockedByBookingId = null "
            + "WHERE s.status = 'LOCKED' AND s.lockedUntil < :now")
    int releaseExpiredLocks(@Param("now") Instant now);
}
