package com.lazycord.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables security for some tests.
 * Activate with @Profile("test") or @ActiveProfiles("test").
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test-no-security")
public class TestSecurityConfig {

    /**
     * Creates a security filter chain that permits all requests.
     * Use this for tests that don't need security.
     */
    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
