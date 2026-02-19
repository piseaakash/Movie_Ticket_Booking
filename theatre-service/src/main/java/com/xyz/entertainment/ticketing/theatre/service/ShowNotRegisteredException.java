package com.xyz.entertainment.ticketing.theatre.service;

public class ShowNotRegisteredException extends RuntimeException {

    public ShowNotRegisteredException(String message) {
        super(message);
    }
}
