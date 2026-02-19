package com.xyz.entertainment.ticketing.booking.client;

/**
 * Thrown when one or more requested seats are not available (e.g. concurrent booking).
 */
public class SeatsNotAvailableException extends RuntimeException {

    public SeatsNotAvailableException(String message) {
        super(message);
    }
}
