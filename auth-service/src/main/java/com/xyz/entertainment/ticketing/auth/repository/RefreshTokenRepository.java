package com.xyz.entertainment.ticketing.auth.repository;

import com.xyz.entertainment.ticketing.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);

    long deleteByExpiresAtBefore(Instant timestamp);
}

