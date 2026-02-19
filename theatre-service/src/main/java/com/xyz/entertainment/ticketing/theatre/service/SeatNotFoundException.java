package com.xyz.entertainment.ticketing.theatre.service;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(String message) {
        super(message);
    }
}
