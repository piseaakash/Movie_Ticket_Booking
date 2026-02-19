package com.xyz.entertainment.ticketing.booking.client;

/**
 * Thrown when theatre-service returns 5xx. Used by Resilience4j retry so we
 * retry only on server errors and timeouts, not on 4xx (e.g. 409 conflict).
 */
public class TheatreServiceRetryableException extends TheatreServiceException {

    public TheatreServiceRetryableException(String message) {
        super(message);
    }

    public TheatreServiceRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
