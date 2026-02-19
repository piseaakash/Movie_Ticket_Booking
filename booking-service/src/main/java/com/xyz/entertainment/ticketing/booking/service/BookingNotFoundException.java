package com.xyz.entertainment.ticketing.booking.service;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }
}

