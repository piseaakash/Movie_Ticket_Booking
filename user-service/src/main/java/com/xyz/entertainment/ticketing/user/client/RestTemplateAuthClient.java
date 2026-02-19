package com.xyz.entertainment.ticketing.user.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate-based implementation of {@link AuthClient}.
 *
 * Tradeoff vs WebClient: RestTemplate is synchronous and blocks the calling thread
 * for the duration of the HTTP call. For login we need the tokens before returning
 * the response anyway, so the flow is naturally synchronous. RestTemplate keeps
 * user-service free of Reactor/WebFlux and is well-suited to a single fire-and-wait
 * call per request. WebClient would be preferable if we had many concurrent outbound
 * calls or needed reactive composition; for one call per login, RestTemplate is simpler.
 */
@Component
public class RestTemplateAuthClient implements AuthClient {

    private static final String TOKENS_PATH = "/api/auth/tokens";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestTemplateAuthClient(
            RestTemplate restTemplate,
            @Value("${auth.service.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public AuthTokenResponse issueTokens(Long userId, String email, java.util.List<String> roles, Long tenantId) {
        String url = baseUrl + TOKENS_PATH;

        AuthTokenRequest request = AuthTokenRequest.builder()
                .userId(userId)
                .email(email)
                .roles(roles != null ? roles : java.util.List.of())
                .tenantId(tenantId)
                .build();

        HttpEntity<AuthTokenRequest> entity = new HttpEntity<>(request);
        try {
            ResponseEntity<AuthTokenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AuthTokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new AuthServiceUnavailableException("Auth service returned " + response.getStatusCode());
        } catch (RestClientException e) {
            throw new AuthServiceUnavailableException("Auth service request failed", e);
        }
    }
}
