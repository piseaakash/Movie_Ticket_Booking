package com.xyz.entertainment.ticketing.booking.service;

public class ForbiddenBookingAccessException extends RuntimeException {

    public ForbiddenBookingAccessException(String message) {
        super(message);
    }
}

