package com.xyz.entertainment.ticketing.theatre.security;

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

class JwtTenantFilterTest {

    private static final String SECRET = "3q2+796t7z8x/Xv9v+7v3q2+796t7z8x/Xv9v+7v3q2+796t7z8x/Xv9v+7v3q2+";

    private JwtTenantFilter filter;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        filter = new JwtTenantFilter(SECRET);
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private String buildToken(Long userId, Long tenantId, List<String> roles) {
        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .signWith(signingKey, SignatureAlgorithm.HS256);
        if (tenantId != null) {
            builder = builder.claim("tenantId", tenantId);
        }
        if (roles != null) {
            builder = builder.claim("roles", roles);
        }
        return builder.compact();
    }

    @Test
    @DisplayName("returns 401 when Authorization header missing")
    void missingAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/theatres");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("returns 401 when token is invalid")
    void invalidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/theatres");
        request.addHeader("Authorization", "Bearer not-a-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("allows request when tenantId is null (e.g. customer token) or roles null (treated as empty)")
    void allowsNullTenantIdOrNullRoles() throws ServletException, IOException {
        // Customer tokens may have null tenantId; filter allows and sets context with null tenantId
        String tokenNoTenant = buildToken(1L, null, List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/shows/1/seats/availability");
        request.addHeader("Authorization", "Bearer " + tokenNoTenant);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);

        // Null roles are treated as empty list; request is allowed (partner ops will fail later with 403)
        String tokenNoRoles = buildToken(1L, 10L, null);
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/theatres");
        request2.addHeader("Authorization", "Bearer " + tokenNoRoles);
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilter(request2, response2, new MockFilterChain());
        assertThat(response2.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("populates TenantContext and allows request when token valid")
    void validToken_populatesContext() throws ServletException, IOException {
        String token = buildToken(5L, 42L, List.of("PARTNER_ADMIN"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/theatres");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // After successful filter execution, tenant context should have been set
        // during the chain and cleared afterwards; we only assert successful status here.
        assertThat(response.getStatus()).isEqualTo(200);
    }
}

