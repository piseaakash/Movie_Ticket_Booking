package com.xyz.entertainment.ticketing.booking.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HTTP client for payment-service. Forwards the current request's Authorization header
 * so payment-service sees the same customer JWT.
 */
@Component
public class PaymentServiceClientImpl implements PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentServiceClientImpl(
            @Qualifier("paymentServiceRestTemplate") RestTemplate restTemplate,
            @Value("${payment.service.base-url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public Long createPayment(Long bookingId, double amount, String currency) {
        String url = baseUrl + "/api/payments";
        Map<String, Object> body = Map.of(
                "bookingId", bookingId,
                "amount", amount,
                "currency", currency);
        ResponseEntity<Map> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headersWithAuth()),
                Map.class);
        if (resp.getBody() != null && resp.getBody().get("id") != null) {
            return ((Number) resp.getBody().get("id")).longValue();
        }
        throw new PaymentServiceClientException("Create payment returned no id");
    }

    @Override
    public void confirmPayment(Long paymentId, String referenceId) {
        String url = baseUrl + "/api/payments/" + paymentId;
        Map<String, Object> body = referenceId != null ? Map.of("referenceId", referenceId) : Map.of();
        restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headersWithAuth()),
                Void.class);
    }

    @Override
    public PaymentStatus getPaymentStatus(Long paymentId) {
        String url = baseUrl + "/api/payments/" + paymentId;
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headersWithAuth()),
                    Map.class);
            if (resp.getBody() != null && resp.getBody().get("status") != null) {
                String status = (String) resp.getBody().get("status");
                return PaymentStatus.valueOf(status);
            }
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return PaymentStatus.NOT_FOUND;
            }
            throw new PaymentServiceClientException("Payment service error: " + e.getStatusCode(), e);
        }
        return PaymentStatus.NOT_FOUND;
    }

    private HttpHeaders headersWithAuth() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String auth = currentAuthorizationHeader();
        if (auth != null) {
            h.set("Authorization", auth);
        }
        return h;
    }

    private static String currentAuthorizationHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getHeader("Authorization");
    }
}
