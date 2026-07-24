package com.example.onboarding.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless REST API — tokens are not sent via cookies, so CSRF does not apply
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // All onboarding endpoints are public: the user has no prior identity
                        // when signing up. See DESIGN.md § Security for the full rationale
                        // and which endpoints must be protected once the auth slice is built.
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
