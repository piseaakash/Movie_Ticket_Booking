package com.xyz.entertainment.ticketing.theatre.service;

/**
 * Thrown when the caller does not have the required role or tries
 * to operate on a theatre outside their tenant.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

