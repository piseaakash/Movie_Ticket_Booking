package com.xyz.entertainment.ticketing.theatre.service;

public class TheatreNotFoundException extends RuntimeException {

    public TheatreNotFoundException(String message) {
        super(message);
    }
}

