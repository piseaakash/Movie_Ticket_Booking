package com.xyz.entertainment.ticketing.booking.client;

import java.util.List;

/**
 * Client for theatre-service seat lock/release/confirm. Abstracts HTTP calls so
 * booking-service can reserve and release seats for concurrent booking safety.
 */
public interface TheatreSeatClient {

    /**
     * Lock seats for the given booking. Call after creating a RESERVED booking.
     * @throws SeatsNotAvailableException if any seat is not available (409)
     * @throws TheatreServiceException on other theatre-service errors
     */
    void lockSeats(Long showId, Long bookingId, List<String> seatLabels, int lockTtlMinutes);

    /**
     * Release seats for the given booking (cancel or expiry).
     */
    void releaseSeats(Long showId, Long bookingId);

    /**
     * Confirm seat lock (convert LOCKED to BOOKED). Call after payment success.
     */
    void confirmSeats(Long showId, Long bookingId);
}
