package com.xyz.entertainment.ticketing.theatre.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Very small JWT filter that validates the access token issued by auth-service,
 * extracts userId (sub), roles and tenantId, and populates {@link TenantContext}.
 *
 * Tradeoff: we avoid bringing in the full Spring Security/OAuth2 resource server
 * stack and instead parse the token directly with JJWT. This keeps theatre-service
 * lightweight but assumes symmetric HS256 signing and shared secret configuration.
 */
@Component
public class JwtTenantFilter extends OncePerRequestFilter {

    private final Key signingKey;

    public JwtTenantFilter(@Value("${auth.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Protect theatre, screen and show inventory APIs; health or actuator can be left open.
        return !path.startsWith("/api/theatres") && !path.startsWith("/api/screens") && !path.startsWith("/api/shows");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
                return;
            }
            String token = authHeader.substring("Bearer ".length()).trim();

            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = jws.getBody();
            Long userId = claims.getSubject() != null ? Long.valueOf(claims.getSubject()) : null;
            Long tenantId = claims.get("tenantId", Long.class);
            List<String> roles = claims.get("roles", List.class);

            if (userId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
                return;
            }
            // tenantId may be null for customer tokens; roles may be empty
            TenantContext.set(userId, tenantId, roles != null ? roles : List.of());
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } catch (Exception ex) {
            // Invalid JWT or parsing failure
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}

