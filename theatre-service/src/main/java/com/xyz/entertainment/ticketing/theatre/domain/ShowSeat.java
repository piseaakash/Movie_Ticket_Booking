package com.xyz.entertainment.ticketing.theatre.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Per-show seat inventory and lock state. Time-bound optimistic locking:
 * LOCKED seats have lockedUntil; expired locks are treated as AVAILABLE.
 */
@Entity
@Table(name = "show_seats")
@IdClass(ShowSeat.ShowSeatId.class)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowSeat {

    @Id
    @Column(name = "show_id")
    private Long showId;

    @Id
    @Column(name = "seat_id")
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "locked_by_booking_id")
    private Long lockedByBookingId;

    public enum Status {
        AVAILABLE,
        LOCKED,
        BOOKED
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShowSeatId implements java.io.Serializable {
        private Long showId;
        private Long seatId;
    }
}
