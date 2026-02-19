package com.xyz.entertainment.ticketing.booking.client;

/**
 * Thrown when payment-service returns an error (4xx/5xx) or call fails.
 */
public class PaymentServiceClientException extends RuntimeException {

    public PaymentServiceClientException(String message) {
        super(message);
    }

    public PaymentServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
