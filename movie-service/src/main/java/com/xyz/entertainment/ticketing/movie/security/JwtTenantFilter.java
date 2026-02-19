package com.xyz.entertainment.ticketing.movie.security;

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
 * JWT filter for partner Show CRUD only. Validates token and sets TenantContext.
 * Browse APIs (/api/movies) are not filtered so customers can use them without token.
 */
@Component
public class JwtTenantFilter extends OncePerRequestFilter {

    private final Key signingKey;

    public JwtTenantFilter(@Value("${auth.jwt.secret:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter partner Show CRUD; leave /api/movies and health open
        return !request.getRequestURI().startsWith("/api/shows");
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
            TenantContext.set(userId, tenantId, roles != null ? roles : List.of());
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } catch (Exception ex) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}
