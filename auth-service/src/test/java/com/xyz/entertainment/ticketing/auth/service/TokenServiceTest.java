package com.xyz.entertainment.ticketing.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.auth.domain.RefreshToken;
import com.xyz.entertainment.ticketing.auth.dto.TokenRequest;
import com.xyz.entertainment.ticketing.auth.dto.TokenResponse;
import com.xyz.entertainment.ticketing.auth.repository.RefreshTokenRepository;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final long ACCESS_TOKEN_SECONDS = 900L;
    private static final long REFRESH_TOKEN_SECONDS = 1209600L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private Key signingKey;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Decoders.BASE64.decode(
                "3q2+796t7z8x/Xv9v+7v3q2+796t7z8x/Xv9v+7v3q2+796t7z8x/Xv9v+7v3q2+");
        signingKey = Keys.hmacShaKeyFor(keyBytes);
        tokenService = new TokenService(
                refreshTokenRepository,
                signingKey,
                SignatureAlgorithm.HS256);
        ReflectionTestUtils.setField(tokenService, "accessTokenSeconds", ACCESS_TOKEN_SECONDS);
        ReflectionTestUtils.setField(tokenService, "refreshTokenSeconds", REFRESH_TOKEN_SECONDS);
    }

    @Nested
    @DisplayName("issueTokens")
    class IssueTokens {

        @Test
        @DisplayName("returns access token, refresh token and expiresIn when roles present")
        void issueTokens_withRoles() {
            TokenRequest request = TokenRequest.builder()
                    .userId(1L)
                    .email("user@example.com")
                    .roles(List.of("CUSTOMER", "ADMIN"))
                    .build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken rt = inv.getArgument(0);
                return RefreshToken.builder()
                        .id(99L)
                        .userId(rt.getUserId())
                        .token(rt.getToken())
                        .expiresAt(rt.getExpiresAt())
                        .createdAt(rt.getCreatedAt())
                        .build();
            });

            TokenResponse response = tokenService.issueTokens(request);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getExpiresInSeconds()).isEqualTo(ACCESS_TOKEN_SECONDS);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getToken()).isEqualTo(response.getRefreshToken());
        }

        @Test
        @DisplayName("handles null roles by using empty list in JWT")
        void issueTokens_withNullRoles() {
            TokenRequest request = TokenRequest.builder()
                    .userId(2L)
                    .email("n roles@example.com")
                    .roles(null)
                    .build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            TokenResponse response = tokenService.issueTokens(request);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getExpiresInSeconds()).isEqualTo(ACCESS_TOKEN_SECONDS);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("handles empty roles list")
        void issueTokens_withEmptyRoles() {
            TokenRequest request = TokenRequest.builder()
                    .userId(3L)
                    .email("empty@example.com")
                    .roles(List.of())
                    .build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            TokenResponse response = tokenService.issueTokens(request);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("returns new access token when refresh token is valid and not expired")
        void refresh_validToken() {
            String tokenValue = "valid-refresh-token-uuid";
            RefreshToken stored = RefreshToken.builder()
                    .id(1L)
                    .userId(10L)
                    .token(tokenValue)
                    .expiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_SECONDS))
                    .createdAt(Instant.now())
                    .build();
            when(refreshTokenRepository.findByToken(eq(tokenValue))).thenReturn(Optional.of(stored));

            TokenResponse response = tokenService.refresh(tokenValue);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isEqualTo(tokenValue);
            assertThat(response.getExpiresInSeconds()).isEqualTo(ACCESS_TOKEN_SECONDS);
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException when token not found")
        void refresh_tokenNotFound() {
            when(refreshTokenRepository.findByToken(eq("missing-token"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tokenService.refresh("missing-token"))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage("Refresh token not found");
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException when token expired")
        void refresh_tokenExpired() {
            String tokenValue = "expired-token";
            RefreshToken stored = RefreshToken.builder()
                    .id(2L)
                    .userId(20L)
                    .token(tokenValue)
                    .expiresAt(Instant.now().minusSeconds(1))
                    .createdAt(Instant.now().minusSeconds(REFRESH_TOKEN_SECONDS))
                    .build();
            when(refreshTokenRepository.findByToken(eq(tokenValue))).thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> tokenService.refresh(tokenValue))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage("Refresh token expired");
        }

        @Test
        @DisplayName("accepts token that expires exactly at now as expired")
        void refresh_tokenExpiresAtNow() {
            String tokenValue = "expires-now";
            Instant now = Instant.now();
            RefreshToken stored = RefreshToken.builder()
                    .id(3L)
                    .userId(30L)
                    .token(tokenValue)
                    .expiresAt(now)
                    .createdAt(now.minusSeconds(3600))
                    .build();
            when(refreshTokenRepository.findByToken(eq(tokenValue))).thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> tokenService.refresh(tokenValue))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage("Refresh token expired");
        }
    }
}
