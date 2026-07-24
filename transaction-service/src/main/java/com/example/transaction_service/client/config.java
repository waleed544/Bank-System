package com.example.transaction_service.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class config {

    @Bean
    public WebClient accountWebClient(WebClient.Builder builder) {

        return builder
                .baseUrl("http://localhost:10000")
                .build();
    }
}