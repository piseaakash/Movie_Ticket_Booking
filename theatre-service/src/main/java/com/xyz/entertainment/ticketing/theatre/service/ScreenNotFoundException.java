package com.xyz.entertainment.ticketing.theatre.service;

public class ScreenNotFoundException extends RuntimeException {

    public ScreenNotFoundException(String message) {
        super(message);
    }
}
