package com.xyz.entertainment.ticketing.booking.security;

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
 * JWT filter for booking-service: requires a CUSTOMER role and extracts userId from sub.
 */
@Component
public class JwtCustomerFilter extends OncePerRequestFilter {

    private final Key signingKey;

    public JwtCustomerFilter(@Value("${auth.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/bookings");
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
            List<String> roles = claims.get("roles", List.class);

            if (userId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
                return;
            }
            if (roles == null || !roles.contains("CUSTOMER")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Customer role required");
                return;
            }

            CustomerContext.setUserId(userId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                CustomerContext.clear();
            }
        } catch (Exception ex) {
            CustomerContext.clear();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}

