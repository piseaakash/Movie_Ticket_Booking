package com.xyz.entertainment.ticketing.payment.service;

public class ForbiddenPaymentAccessException extends RuntimeException {

    public ForbiddenPaymentAccessException(String message) {
        super(message);
    }
}
