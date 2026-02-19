package com.xyz.entertainment.ticketing.payment.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.xyz.entertainment.ticketing.payment.service.ForbiddenPaymentAccessException;
import com.xyz.entertainment.ticketing.payment.service.PaymentNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    @DisplayName("maps PaymentNotFoundException to 404")
    void handlePaymentNotFound() {
        ResponseEntity<Object> response = handler.handlePaymentNotFound(
                new PaymentNotFoundException("Payment not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Payment not found");
    }

    @Test
    @DisplayName("maps ForbiddenPaymentAccessException to 403")
    void handleForbidden() {
        ResponseEntity<Object> response = handler.handleForbidden(
                new ForbiddenPaymentAccessException("Payment does not belong to current user"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Forbidden");
    }

    @Test
    @DisplayName("maps generic Exception to 500")
    void handleGeneric() {
        ResponseEntity<Object> response = handler.handleGeneric(new RuntimeException("boom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Internal server error");
    }
}
