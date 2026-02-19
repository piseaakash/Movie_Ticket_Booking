package com.xyz.entertainment.ticketing.user.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.xyz.entertainment.ticketing.user.service.InvalidCredentialsException;
import com.xyz.entertainment.ticketing.user.service.UserAlreadyExistsException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.core.MethodParameter;

class RestExceptionHandlerTest {

    @Test
    void handleUserAlreadyExists_returns_conflict() {
        RestExceptionHandler handler = new RestExceptionHandler();
        UserAlreadyExistsException ex = new UserAlreadyExistsException("User with email already exists");

        ResponseEntity<Object> response = handler.handleUserAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("error")).isEqualTo("User already exists");
    }

    @Test
    void handleInvalidCredentials_returns_unauthorized() {
        RestExceptionHandler handler = new RestExceptionHandler();
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid email or password");

        ResponseEntity<Object> response = handler.handleInvalidCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Invalid credentials");
    }

    @Test
    void handleGeneric_returns_internal_server_error() {
        RestExceptionHandler handler = new RestExceptionHandler();
        RuntimeException ex = new RuntimeException("Unexpected");

        ResponseEntity<Object> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Internal server error");
    }

    @Test
    void handleMethodArgumentNotValid_returns_field_errors() throws NoSuchMethodException {
        RestExceptionHandler handler = new RestExceptionHandler();

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));

        MethodParameter parameter = new MethodParameter(
                DummyController.class.getDeclaredMethod("dummyMethod", String.class), 0);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Validation failed");
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) body.get("fieldErrors");
        assertThat(fieldErrors).containsEntry("email", "must not be blank");
    }

    private static final class DummyController {
        @SuppressWarnings("unused")
        void dummyMethod(String email) {
            // no-op
        }
    }
}

