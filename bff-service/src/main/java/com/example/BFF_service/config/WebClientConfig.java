package com.example.BFF_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient userWebClient(
            WebClient.Builder builder,
            @Value("${services.user-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient accountWebClient(
            WebClient.Builder builder,
            @Value("${services.account-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient transactionWebClient(
            WebClient.Builder builder,
            @Value("${services.transaction-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}