package com.xyz.entertainment.ticketing.movie.service;

/**
 * Thrown when the caller is not allowed to perform the operation (e.g. tenant mismatch).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
