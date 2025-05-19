package com.SimonMk116.gendev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply CORS
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF if your API is stateless, typical for SSE
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/offers/**").permitAll() // <<<--- THIS LINE MAKES IT PUBLIC
                        .anyRequest().authenticated() // All other requests might still need authentication
                )
                .formLogin(form -> form // If you still have form login for other parts
                        .loginPage("/login") // Ensure this is a relative path
                        .permitAll()
                );
        // Add other configurations as needed (e.g., .oauth2Login(), .httpBasic(), etc.)
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://6000-firebase-studio-1747694501106.cluster-6vyo4gb53jczovun3dxslzjahs.cloudworkstations.dev",
                "http://localhost:9002" // For your local Next.js dev
                // Add other origins if needed
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true); // Important if your frontend sends credentials
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }
}
