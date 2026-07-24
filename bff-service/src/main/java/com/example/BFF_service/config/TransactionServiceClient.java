package com.example.BFF_service.config;

import com.example.BFF_service.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final WebClient transactionWebClient;


    public Flux<TransactionResponse> getTransactions(UUID accountId) {
        return transactionWebClient
                .get()
                .uri("/accounts/{accountId}/transactions", accountId)
                .retrieve()
                .bodyToFlux(TransactionResponse.class)
                .onErrorResume(ex -> Flux.empty()); // 404 → empty list, don't fail dashboard
    }
}