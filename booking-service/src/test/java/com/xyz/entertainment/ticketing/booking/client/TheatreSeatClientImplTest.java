package com.xyz.entertainment.ticketing.booking.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class TheatreSeatClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private TheatreSeatClientImpl client() {
        return new TheatreSeatClientImpl(restTemplate, "http://theatre:8082");
    }

    @Nested
    @DisplayName("lockSeats")
    class LockSeats {

        @Test
        @DisplayName("throws SeatsNotAvailableException when theatre returns 409")
        void on409() {
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Void.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

            var client = client();
            assertThatThrownBy(() -> client.lockSeats(1L, 10L, List.of("A1"), 15))
                    .isInstanceOf(SeatsNotAvailableException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("calls correct URL")
        void callsCorrectUrl() {
            var client = client();
            client.lockSeats(5L, 20L, List.of("A1", "A2"), 10);

            verify(restTemplate).postForObject(
                    eq("http://theatre:8082/api/shows/5/seats/lock"),
                    any(HttpEntity.class),
                    eq(Void.class));
        }
    }

    @Nested
    @DisplayName("releaseSeats")
    class ReleaseSeats {

        @Test
        @DisplayName("calls correct URL")
        void callsCorrectUrl() {
            var client = client();
            client.releaseSeats(3L, 7L);

            verify(restTemplate).postForObject(
                    eq("http://theatre:8082/api/shows/3/seats/release"),
                    any(HttpEntity.class),
                    eq(Void.class));
        }
    }

    @Nested
    @DisplayName("confirmSeats")
    class ConfirmSeats {

        @Test
        @DisplayName("calls correct URL")
        void callsCorrectUrl() {
            var client = client();
            client.confirmSeats(2L, 11L);

            verify(restTemplate).postForObject(
                    eq("http://theatre:8082/api/shows/2/seats/confirm"),
                    any(HttpEntity.class),
                    eq(Void.class));
        }
    }
}
