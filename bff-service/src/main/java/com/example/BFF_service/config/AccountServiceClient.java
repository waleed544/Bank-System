package com.example.BFF_service.config;

import com.example.BFF_service.dto.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final WebClient accountWebClient;


    public Flux<AccountResponse> getAccounts(UUID userId) {
        return accountWebClient
                .get()
                .uri("/users/{userId}/accounts", userId)
                .retrieve()
                .bodyToFlux(AccountResponse.class);
    }
}