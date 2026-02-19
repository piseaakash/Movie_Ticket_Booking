package com.xyz.entertainment.ticketing.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityBeansConfigTest {

    @Test
    void passwordEncoder_is_bcrypt_and_encodes_passwords() {
        SecurityBeansConfig config = new SecurityBeansConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

        String raw = "myPassword!";
        String hash = encoder.encode(raw);

        assertThat(hash).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hash)).isTrue();
    }
}

