package com.example.transaction_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class config {

    @Bean
    public WebClient accountWebClient(
            WebClient.Builder builder,
            @Value("${services.account-service.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
