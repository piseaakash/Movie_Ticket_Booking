package com.xyz.entertainment.ticketing.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security-related beans used by the user service.
 *
 * Note: We only use Spring Security's crypto module here for password hashing.
 * Full authentication/authorization concerns (filters, access rules, JWTs) are
 * intentionally left to a dedicated auth service.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is a battle-tested adaptive hashing algorithm widely used in production.
        // The default strength is a reasonable trade-off between security and performance
        // for a small demo application.
        return new BCryptPasswordEncoder();
    }
}

