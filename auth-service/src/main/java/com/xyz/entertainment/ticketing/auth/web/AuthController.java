package com.xyz.entertainment.ticketing.auth.web;

import com.xyz.entertainment.ticketing.auth.dto.TokenRequest;
import com.xyz.entertainment.ticketing.auth.dto.TokenResponse;
import com.xyz.entertainment.ticketing.auth.service.InvalidRefreshTokenException;
import com.xyz.entertainment.ticketing.auth.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final TokenService tokenService;

    @PostMapping("/tokens")
    public ResponseEntity<TokenResponse> issueTokens(@Valid @RequestBody TokenRequest request) {
        TokenResponse response = tokenService.issueTokens(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public record RefreshRequest(
            @NotBlank @Size(max = 128) String refreshToken
    ) { }

    @PostMapping("/tokens/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = tokenService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<String> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}

