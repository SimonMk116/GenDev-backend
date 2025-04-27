package com.SimonMk116.gendev.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // This will allow CORS for all endpoints in your application
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")  // React dev server URL (adjust)
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

