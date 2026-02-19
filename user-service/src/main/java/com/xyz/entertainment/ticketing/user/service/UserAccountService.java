package com.xyz.entertainment.ticketing.user.service;

import com.xyz.entertainment.ticketing.user.domain.UserAccount;
import com.xyz.entertainment.ticketing.user.dto.LoginRequest;
import com.xyz.entertainment.ticketing.user.dto.RegisterUserRequest;
import com.xyz.entertainment.ticketing.user.dto.UserResponse;
import com.xyz.entertainment.ticketing.user.repository.UserAccountRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email already exists");
        }

        Instant now = Instant.now();
        UserAccount account = UserAccount.builder()
                .email(normalizedEmail)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserAccount saved = repository.save(account);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        UserAccount account = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // JWT or other access token will be issued by a dedicated auth service.
        return toResponse(account);
    }

    private static UserResponse toResponse(UserAccount account) {
        return UserResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .build();
    }
}

