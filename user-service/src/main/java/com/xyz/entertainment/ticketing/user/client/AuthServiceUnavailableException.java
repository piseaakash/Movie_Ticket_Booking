package com.xyz.entertainment.ticketing.user.client;

/**
 * Thrown when the auth-service cannot be reached or returns an error
 * during token issuance (e.g. 5xx or connection failure).
 */
public class AuthServiceUnavailableException extends RuntimeException {

    public AuthServiceUnavailableException(String message) {
        super(message);
    }

    public AuthServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
