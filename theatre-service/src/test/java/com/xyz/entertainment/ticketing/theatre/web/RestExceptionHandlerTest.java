package com.xyz.entertainment.ticketing.theatre.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.xyz.entertainment.ticketing.theatre.service.ForbiddenException;
import com.xyz.entertainment.ticketing.theatre.service.ScreenNotFoundException;
import com.xyz.entertainment.ticketing.theatre.service.SeatsNotAvailableException;
import com.xyz.entertainment.ticketing.theatre.service.TheatreNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    @DisplayName("maps ForbiddenException to 403")
    void handleForbidden() {
        ResponseEntity<Object> response = handler.handleForbidden(new ForbiddenException("nope"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Forbidden");
    }

    @Test
    @DisplayName("maps TheatreNotFoundException to 404")
    void handleNotFound_theatre() {
        ResponseEntity<Object> response = handler.handleNotFound(new TheatreNotFoundException("missing"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Not Found");
    }

    @Test
    @DisplayName("maps ScreenNotFoundException to 404")
    void handleNotFound_screen() {
        ResponseEntity<Object> response = handler.handleNotFound(new ScreenNotFoundException("Screen not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Not Found");
    }

    @Test
    @DisplayName("maps SeatsNotAvailableException to 409")
    void handleSeatsNotAvailable() {
        ResponseEntity<Object> response = handler.handleSeatsNotAvailable(
                new SeatsNotAvailableException("One or more seats not available"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Seats not available");
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

