package com.SimonMk116.gendev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .anyRequest().permitAll()
                                /*.requestMatchers("/api/offers/**").permitAll() // Allow unauthenticated access to /api/offers
                                .requestMatchers(HttpMethod.POST, "/api/user-activity/log-search").permitAll()
                                .anyRequest().authenticated() // All other requests require authentication*/
                )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF if your API is stateless, common for SSE
                //.cors(withDefaults()) // Apply CORS configuration (uses @CrossOrigin on the controller)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)); // Or .sameOrigin() if needed
        return http.build();
    }
    /**
     * Defines a global CORS configuration source.
     * Adjust allowed origins, methods, and headers based on your frontend deployment.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // Allow credentials (cookies, HTTP authentication) to be sent with cross-origin requests
        config.setAllowCredentials(true);
        // Set allowed origins. In production, replace "*" with your actual frontend domain(s).
        // For multiple origins: Arrays.asList("http://your-frontend.com", "http://another-domain.com")
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://your-production-frontend.com")); // Replace with actual domains
        // Define allowed HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        // Define allowed request headers
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Auth-Token", "X-Client-Id", "X-Timestamp", "X-Signature"));
        // Define exposed headers (headers that the client can access)
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Auth-Token", "X-Client-Id", "X-Timestamp", "X-Signature"));
        // Max age for pre-flight requests (in seconds)
        config.setMaxAge(3600L); // 1 hour
        source.registerCorsConfiguration("/**", config); // Apply this CORS config to all paths
        return source;
    }
}
