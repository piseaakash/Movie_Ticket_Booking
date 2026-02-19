package com.xyz.entertainment.ticketing.theatre.service;

/**
 * Thrown when one or more requested seats could not be locked (already taken or invalid).
 */
public class SeatsNotAvailableException extends RuntimeException {

    public SeatsNotAvailableException(String message) {
        super(message);
    }
}
