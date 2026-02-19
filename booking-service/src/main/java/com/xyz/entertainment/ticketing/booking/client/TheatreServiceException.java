package com.xyz.entertainment.ticketing.booking.client;

/**
 * Thrown when theatre-service returns an error (e.g. show not registered, 404).
 */
public class TheatreServiceException extends RuntimeException {

    public TheatreServiceException(String message) {
        super(message);
    }

    public TheatreServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
