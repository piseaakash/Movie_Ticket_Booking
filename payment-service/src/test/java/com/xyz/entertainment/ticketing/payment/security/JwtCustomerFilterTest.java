package com.xyz.entertainment.ticketing.payment.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.security.Key;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtCustomerFilterTest {

    private static final String SECRET = "3q2+796t7z8x/Xv9v+7v3q2+796t7z8x/Xv9v+7v3q2+796t7z8x/Xv9v+7v3q2+";

    private JwtCustomerFilter filter;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        filter = new JwtCustomerFilter(SECRET);
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    private String buildToken(Long userId, List<String> roles) {
        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .signWith(signingKey, SignatureAlgorithm.HS256);
        if (roles != null) {
            builder = builder.claim("roles", roles);
        }
        return builder.compact();
    }

    @Test
    @DisplayName("does not filter when path is not under /api/payments")
    void shouldNotFilter_nonPaymentPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/other");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("returns 401 when Authorization header missing")
    void missingAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("returns 401 when token is invalid")
    void invalidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payments");
        request.addHeader("Authorization", "Bearer not-a-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("returns 403 when CUSTOMER role missing")
    void missingCustomerRole() throws ServletException, IOException {
        String token = buildToken(1L, List.of("PARTNER_ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payments");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("allows request when token has CUSTOMER role")
    void validToken_customerRole() throws ServletException, IOException {
        String token = buildToken(5L, List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payments");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
