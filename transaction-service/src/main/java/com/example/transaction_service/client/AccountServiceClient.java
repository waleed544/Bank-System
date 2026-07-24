package com.example.transaction_service.client;

import com.example.transaction_service.dto.request.AccountTransferRequest;
import com.example.transaction_service.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.transaction_service.dto.response.AccountResponse;
import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final WebClient accountWebClient;

    public MessageResponse transfer(UUID fromAccountId,
                                    UUID toAccountId,
                                    BigDecimal amount) {

        AccountTransferRequest request = AccountTransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .build();

        return accountWebClient
                .put()
                .uri("/accounts/transfer")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MessageResponse.class)
                .block();
    }

    public AccountResponse getAccount(UUID accountId) {

        return accountWebClient
                .get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();
    }
}