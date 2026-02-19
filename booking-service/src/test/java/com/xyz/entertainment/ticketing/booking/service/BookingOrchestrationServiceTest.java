package com.xyz.entertainment.ticketing.booking.service;

import com.xyz.entertainment.ticketing.booking.api.model.BookingResponse;
import com.xyz.entertainment.ticketing.booking.api.model.CreateBookingRequest;
import com.xyz.entertainment.ticketing.booking.client.PaymentServiceClient;
import com.xyz.entertainment.ticketing.booking.client.PaymentServiceClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOrchestrationServiceTest {

    @Mock
    private BookingService bookingService;
    @Mock
    private PaymentServiceClient paymentServiceClient;

    private BookingOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new BookingOrchestrationService(bookingService, paymentServiceClient);
    }

    @Test
    void reserveBooking_success_createsBookingThenPayment() {
        CreateBookingRequest request = new CreateBookingRequest()
                .showId(1L).tenantId(2L).seats(java.util.List.of("A1"));
        BookingResponse booking = new BookingResponse().id(10L).showId(1L).tenantId(2L).status(BookingResponse.StatusEnum.RESERVED);
        when(bookingService.createBooking(any())).thenReturn(booking);
        when(paymentServiceClient.createPayment(10L, 250.0, "INR")).thenReturn(20L);

        var result = orchestrationService.reserveBooking(request, 250.0, "INR");

        assertThat(result.booking().getId()).isEqualTo(10L);
        assertThat(result.paymentId()).isEqualTo(20L);
        verify(bookingService).createBooking(request);
        verify(paymentServiceClient).createPayment(10L, 250.0, "INR");
        verify(bookingService, never()).cancelBooking(any());
    }

    @Test
    void reserveBooking_paymentCreateFails_cancelsBookingAndThrows() {
        CreateBookingRequest request = new CreateBookingRequest()
                .showId(1L).tenantId(2L).seats(java.util.List.of("A1"));
        BookingResponse booking = new BookingResponse().id(10L).showId(1L).tenantId(2L).status(BookingResponse.StatusEnum.RESERVED);
        when(bookingService.createBooking(any())).thenReturn(booking);
        when(paymentServiceClient.createPayment(10L, 250.0, "INR"))
                .thenThrow(new PaymentServiceClientException("Payment service down"));

        assertThatThrownBy(() -> orchestrationService.reserveBooking(request, 250.0, "INR"))
                .isInstanceOf(PaymentServiceClientException.class)
                .hasMessageContaining("Payment service down");

        verify(bookingService).createBooking(request);
        verify(paymentServiceClient).createPayment(10L, 250.0, "INR");
        verify(bookingService).cancelBooking(10L);
    }

    @Test
    void confirmPaymentAndBooking_success_confirmsPaymentThenBooking() {
        when(paymentServiceClient.getPaymentStatus(20L)).thenReturn(PaymentServiceClient.PaymentStatus.PENDING);
        BookingResponse confirmed = new BookingResponse().id(10L).status(BookingResponse.StatusEnum.CONFIRMED);
        when(bookingService.confirmBooking(10L)).thenReturn(confirmed);

        BookingResponse result = orchestrationService.confirmPaymentAndBooking(10L, 20L);

        assertThat(result.getStatus()).isEqualTo(BookingResponse.StatusEnum.CONFIRMED);
        verify(paymentServiceClient).confirmPayment(20L, null);
        verify(bookingService).confirmBooking(10L);
        verify(bookingService, never()).cancelBooking(any());
    }

    @Test
    void confirmPaymentAndBooking_paymentFailed_cancelsBookingAndThrows() {
        when(paymentServiceClient.getPaymentStatus(20L)).thenReturn(PaymentServiceClient.PaymentStatus.FAILED);

        assertThatThrownBy(() -> orchestrationService.confirmPaymentAndBooking(10L, 20L))
                .isInstanceOf(PaymentServiceClientException.class)
                .hasMessageContaining("FAILED");

        verify(bookingService).cancelBooking(10L);
        verify(paymentServiceClient, never()).confirmPayment(any(), any());
        verify(bookingService, never()).confirmBooking(any());
    }

    @Test
    void confirmPaymentAndBooking_confirmPaymentFails_cancelsBookingAndThrows() {
        when(paymentServiceClient.getPaymentStatus(20L)).thenReturn(PaymentServiceClient.PaymentStatus.PENDING);
        doThrow(new PaymentServiceClientException("Already confirmed"))
                .when(paymentServiceClient).confirmPayment(20L, null);

        assertThatThrownBy(() -> orchestrationService.confirmPaymentAndBooking(10L, 20L))
                .isInstanceOf(PaymentServiceClientException.class);

        verify(paymentServiceClient).confirmPayment(20L, null);
        verify(bookingService).cancelBooking(10L);
        verify(bookingService, never()).confirmBooking(any());
    }
}
