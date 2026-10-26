package com.restaurante.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${menu.service.url:http://localhost:8080}")
    private String menuUrl;

    @Value("${menu.service.timeout.seconds:3}")
    private int timeoutSeconds;

    @Bean
    public WebClient menuWebClient() {
        return WebClient.builder().baseUrl(menuUrl).build();
    }

    public Duration getTimeout() { return Duration.ofSeconds(timeoutSeconds); }
}
