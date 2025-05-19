package com.SimonMk116.gendev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/offers/**").permitAll() // Allow unauthenticated access to /api/offers
                                .anyRequest().authenticated() // All other requests require authentication
                )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF if your API is stateless, common for SSE
                .cors(withDefaults()); // Apply CORS configuration (uses your @CrossOrigin on the controller)
        return http.build();
    }
}