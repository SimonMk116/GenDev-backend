package com.SimonMk116.gendev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${provider.servus.base-url}")
    private String baseUrl;

    @Value("${provider.servus.username}")
    private String username;

    @Value("${provider.servus.password}")
    private String password;

    @Bean(name = "servusSpeedWebClient")
    public WebClient servusSpeedWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }
}
