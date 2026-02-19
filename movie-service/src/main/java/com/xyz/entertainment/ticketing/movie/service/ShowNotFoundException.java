package com.xyz.entertainment.ticketing.movie.service;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(String message) {
        super(message);
    }
}
