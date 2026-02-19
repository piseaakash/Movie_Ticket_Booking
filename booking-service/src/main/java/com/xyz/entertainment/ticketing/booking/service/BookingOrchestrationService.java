package com.xyz.entertainment.ticketing.booking.service;

import com.xyz.entertainment.ticketing.booking.api.model.BookingResponse;
import com.xyz.entertainment.ticketing.booking.api.model.CreateBookingRequest;
import com.xyz.entertainment.ticketing.booking.client.PaymentServiceClient;
import com.xyz.entertainment.ticketing.booking.client.PaymentServiceClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrator for reserve -> payment and confirm-payment -> confirm-booking flows.
 * Saga-style: on payment create failure we compensate by cancelling the booking;
 * on confirm-payment failure we cancel the booking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOrchestrationService {

    private final BookingService bookingService;
    private final PaymentServiceClient paymentServiceClient;

    /**
     * Reserve booking (create booking + create payment). If payment creation fails,
     * compensates by cancelling the booking.
     *
     * @param request same as create booking, plus amount and currency for payment
     * @return created booking and payment id (caller can use for confirm-payment step)
     */
    public ReserveBookingResult reserveBooking(CreateBookingRequest request, double amount, String currency) {
        BookingResponse booking = bookingService.createBooking(request);
        Long bookingId = booking.getId();
        try {
            Long paymentId = paymentServiceClient.createPayment(bookingId, amount, currency);
            return new ReserveBookingResult(booking, paymentId);
        } catch (PaymentServiceClientException e) {
            log.warn("Payment create failed for booking {}; cancelling booking", bookingId, e);
            try {
                bookingService.cancelBooking(bookingId);
            } catch (Exception ex) {
                log.error("Compensating cancel failed for booking {}", bookingId, ex);
            }
            throw e;
        }
    }

    /**
     * Confirm payment then confirm booking. On payment failure (e.g. already FAILED or 400),
     * cancels the booking and throws.
     */
    public BookingResponse confirmPaymentAndBooking(Long bookingId, Long paymentId) {
        PaymentServiceClient.PaymentStatus status = paymentServiceClient.getPaymentStatus(paymentId);
        if (status == PaymentServiceClient.PaymentStatus.FAILED
                || status == PaymentServiceClient.PaymentStatus.NOT_FOUND) {
            cancelBookingAndThrow(bookingId, status);
        }
        try {
            paymentServiceClient.confirmPayment(paymentId, null);
        } catch (PaymentServiceClientException e) {
            log.warn("Confirm payment failed for payment {}; cancelling booking {}", paymentId, bookingId, e);
            try {
                bookingService.cancelBooking(bookingId);
            } catch (Exception ex) {
                log.error("Compensating cancel failed for booking {}", bookingId, ex);
            }
            throw e;
        }
        return bookingService.confirmBooking(bookingId);
    }

    private void cancelBookingAndThrow(Long bookingId, PaymentServiceClient.PaymentStatus status) {
        try {
            bookingService.cancelBooking(bookingId);
        } catch (Exception e) {
            log.error("Cancel booking {} after payment {} failed", bookingId, status, e);
        }
        throw new PaymentServiceClientException(
                "Payment is " + status + "; booking " + bookingId + " has been cancelled");
    }

    public record ReserveBookingResult(BookingResponse booking, Long paymentId) {}
}
