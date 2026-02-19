package com.xyz.entertainment.ticketing.auth.web;

import com.xyz.entertainment.ticketing.auth.dto.TokenRequest;
import com.xyz.entertainment.ticketing.auth.dto.TokenResponse;
import com.xyz.entertainment.ticketing.auth.service.InvalidRefreshTokenException;
import com.xyz.entertainment.ticketing.auth.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @Nested
    @DisplayName("POST /api/auth/tokens")
    class IssueTokens {

        @Test
        @DisplayName("returns 201 and token response when request valid")
        void issueTokens_created() throws Exception {
            TokenResponse response = TokenResponse.builder()
                    .accessToken("access-jwt")
                    .refreshToken("refresh-uuid")
                    .expiresInSeconds(900L)
                    .build();
            when(tokenService.issueTokens(any(TokenRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/tokens")
                            .contentType(APPLICATION_JSON)
                            .content("{\"userId\":1,\"email\":\"u@e.com\",\"roles\":[\"CUSTOMER\"]}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-uuid"))
                    .andExpect(jsonPath("$.expiresInSeconds").value(900));

            verify(tokenService).issueTokens(any(TokenRequest.class));
        }

        @Test
        @DisplayName("returns 400 when userId missing")
        void issueTokens_missingUserId() throws Exception {
            mockMvc.perform(post("/api/auth/tokens")
                            .contentType(APPLICATION_JSON)
                            .content("{\"email\":\"u@e.com\",\"roles\":[\"CUSTOMER\"]}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email blank")
        void issueTokens_blankEmail() throws Exception {
            mockMvc.perform(post("/api/auth/tokens")
                            .contentType(APPLICATION_JSON)
                            .content("{\"userId\":1,\"email\":\"\",\"roles\":[\"CUSTOMER\"]}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email missing")
        void issueTokens_missingEmail() throws Exception {
            mockMvc.perform(post("/api/auth/tokens")
                            .contentType(APPLICATION_JSON)
                            .content("{\"userId\":1,\"roles\":[\"CUSTOMER\"]}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/tokens/refresh")
    class Refresh {

        @Test
        @DisplayName("returns 200 and token response when refresh token valid")
        void refresh_ok() throws Exception {
            TokenResponse response = TokenResponse.builder()
                    .accessToken("new-access")
                    .refreshToken("same-refresh")
                    .expiresInSeconds(900L)
                    .build();
            when(tokenService.refresh(eq("my-refresh-token"))).thenReturn(response);

            mockMvc.perform(post("/api/auth/tokens/refresh")
                            .contentType(APPLICATION_JSON)
                            .content("{\"refreshToken\":\"my-refresh-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.refreshToken").value("same-refresh"))
                    .andExpect(jsonPath("$.expiresInSeconds").value(900));

            verify(tokenService).refresh("my-refresh-token");
        }

        @Test
        @DisplayName("returns 401 when refresh token invalid")
        void refresh_unauthorized() throws Exception {
            when(tokenService.refresh(eq("bad-token")))
                    .thenThrow(new InvalidRefreshTokenException("Refresh token not found"));

            mockMvc.perform(post("/api/auth/tokens/refresh")
                            .contentType(APPLICATION_JSON)
                            .content("{\"refreshToken\":\"bad-token\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Refresh token not found"));

            verify(tokenService).refresh("bad-token");
        }

        @Test
        @DisplayName("returns 401 when refresh token expired")
        void refresh_expired() throws Exception {
            when(tokenService.refresh(eq("expired-token")))
                    .thenThrow(new InvalidRefreshTokenException("Refresh token expired"));

            mockMvc.perform(post("/api/auth/tokens/refresh")
                            .contentType(APPLICATION_JSON)
                            .content("{\"refreshToken\":\"expired-token\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Refresh token expired"));
        }

        @Test
        @DisplayName("returns 400 when refreshToken missing")
        void refresh_missingRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/tokens/refresh")
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when refreshToken blank")
        void refresh_blankRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/tokens/refresh")
                            .contentType(APPLICATION_JSON)
                            .content("{\"refreshToken\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
