package com.xyz.entertainment.ticketing.auth.service;

import com.xyz.entertainment.ticketing.auth.domain.RefreshToken;
import com.xyz.entertainment.ticketing.auth.dto.TokenRequest;
import com.xyz.entertainment.ticketing.auth.dto.TokenResponse;
import com.xyz.entertainment.ticketing.auth.repository.RefreshTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Key signingKey;
    private final SignatureAlgorithm signatureAlgorithm;

    @Value("${auth.jwt.access-token-seconds}")
    private long accessTokenSeconds;

    @Value("${auth.jwt.refresh-token-seconds}")
    private long refreshTokenSeconds;

    @Transactional
    public TokenResponse issueTokens(TokenRequest request) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
                .setSubject(String.valueOf(request.getUserId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenSeconds, ChronoUnit.SECONDS)))
                .claim("email", request.getEmail())
                .claim("roles", request.getRoles() != null ? request.getRoles() : List.of());
        if (request.getTenantId() != null) {
            builder = builder.claim("tenantId", request.getTenantId());
        }
        String accessToken = builder.signWith(signingKey, signatureAlgorithm).compact();

        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(request.getUserId())
                .token(refreshTokenValue)
                .createdAt(now)
                .expiresAt(now.plus(refreshTokenSeconds, ChronoUnit.SECONDS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresInSeconds(accessTokenSeconds)
                .build();
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (!stored.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // For simplicity we do not rotate refresh tokens here; in a real system
        // you would typically rotate and invalidate the old one.

        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(stored.getUserId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenSeconds, ChronoUnit.SECONDS)))
                .signWith(signingKey, signatureAlgorithm)
                .compact();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresInSeconds(accessTokenSeconds)
                .build();
    }
}

