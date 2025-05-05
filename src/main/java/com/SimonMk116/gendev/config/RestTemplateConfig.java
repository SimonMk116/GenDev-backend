package com.SimonMk116.gendev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class RestTemplateConfig {

    @Value("${provider.servus.username}")
    private String username;

    @Value("${provider.servus.password}")
    private String password;

    @Bean
    public RestTemplate restTemplate() {
        //configure custom timeouts, interceptors, etc. for RestTemplate here
        return new RestTemplate();
    }

    @Bean(name = "servusSpeedRestTemplate")
    public RestTemplate servusSpeedRestTemplate(RestTemplateBuilder builder) {
        return builder.basicAuthentication(username, password).build();
    }

}

