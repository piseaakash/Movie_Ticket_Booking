package com.xyz.entertainment.ticketing.booking.client;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Calls theatre-service to lock, release, and confirm seats.
 * Retry: up to 2 retries (3 attempts total) on 5xx or timeout.
 * Circuit breaker: opens after N failures to avoid cascading when theatre-service is down.
 */
@Component
public class TheatreSeatClientImpl implements TheatreSeatClient {

    private static final String THEATRE_SEAT_CLIENT = "theatreSeatClient";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TheatreSeatClientImpl(
            RestTemplate restTemplate,
            @Value("${theatre.service.base-url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    @Retry(name = THEATRE_SEAT_CLIENT)
    @CircuitBreaker(name = THEATRE_SEAT_CLIENT)
    public void lockSeats(Long showId, Long bookingId, List<String> seatLabels, int lockTtlMinutes) {
        String url = baseUrl + "/api/shows/" + showId + "/seats/lock";
        Map<String, Object> body = Map.of(
                "bookingId", bookingId,
                "seatLabels", seatLabels,
                "lockTtlMinutes", lockTtlMinutes);
        try {
            restTemplate.postForObject(url, new HttpEntity<>(body, jsonHeaders()), Void.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new SeatsNotAvailableException("One or more seats are not available");
            }
            throw toTheatreException(e);
        }
    }

    @Override
    @Retry(name = THEATRE_SEAT_CLIENT)
    @CircuitBreaker(name = THEATRE_SEAT_CLIENT)
    public void releaseSeats(Long showId, Long bookingId) {
        String url = baseUrl + "/api/shows/" + showId + "/seats/release";
        Map<String, Object> body = Map.of("bookingId", bookingId);
        try {
            restTemplate.postForObject(url, new HttpEntity<>(body, jsonHeaders()), Void.class);
        } catch (HttpStatusCodeException e) {
            throw toTheatreException(e);
        }
    }

    @Override
    @Retry(name = THEATRE_SEAT_CLIENT)
    @CircuitBreaker(name = THEATRE_SEAT_CLIENT)
    public void confirmSeats(Long showId, Long bookingId) {
        String url = baseUrl + "/api/shows/" + showId + "/seats/confirm";
        Map<String, Object> body = Map.of("bookingId", bookingId);
        try {
            restTemplate.postForObject(url, new HttpEntity<>(body, jsonHeaders()), Void.class);
        } catch (HttpStatusCodeException e) {
            throw toTheatreException(e);
        }
    }

    /** 5xx → retryable; 4xx → non-retryable so we do not retry on conflict/bad request. */
    private static RuntimeException toTheatreException(HttpStatusCodeException e) {
        if (e.getStatusCode().is5xxServerError()) {
            return new TheatreServiceRetryableException("Theatre service error: " + e.getStatusCode(), e);
        }
        return new TheatreServiceException("Theatre service error: " + e.getStatusCode(), e);
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
