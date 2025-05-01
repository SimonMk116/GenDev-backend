package com.SimonMk116.gendev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean(name = "servusSpeedWebClient")
    public WebClient servusSpeedWebClient() {
        return WebClient.builder()
                .baseUrl("https://servus-speed.gendev7.check24.fun")
                .defaultHeaders(headers -> headers.setBasicAuth("user_36DCB2807C4D", "D3153DFAA379"))
                .build();
    }
}
