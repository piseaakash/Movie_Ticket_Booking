package com.xyz.entertainment.ticketing.booking.client;

/**
 * Client for payment-service: create, confirm, and get payment.
 * Used by the booking orchestrator. Calls are made with the current customer's JWT forwarded.
 */
public interface PaymentServiceClient {

    /**
     * Create a PENDING payment for the given booking. JWT is forwarded so payment is owned by current user.
     *
     * @return payment id
     */
    Long createPayment(Long bookingId, double amount, String currency);

    /**
     * Confirm payment (PENDING → CONFIRMED). Optional referenceId (e.g. gateway transaction id).
     */
    void confirmPayment(Long paymentId, String referenceId);

    /**
     * Get payment status (e.g. to check if FAILED before confirming booking).
     */
    PaymentStatus getPaymentStatus(Long paymentId);

    /** Minimal payment view for orchestrator. */
    enum PaymentStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        NOT_FOUND
    }
}
