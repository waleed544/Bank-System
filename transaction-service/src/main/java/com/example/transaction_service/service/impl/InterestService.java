package com.example.transaction_service.service.impl;

import com.example.transaction_service.dto.request.TransferExecutionRequest;
import com.example.transaction_service.dto.request.TransferInitiationRequest;
import com.example.transaction_service.dto.response.AccountResponse;
import com.example.transaction_service.dto.response.TransferInitiationResponse;
import com.example.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestService {

    private final TransactionService transactionService;
    private final WebClient webClient;

    @Value("${account.service.url:http://localhost:10000}")
    private String accountServiceUrl;

    public void creditDailyInterest() {

        // Fetch the SYSTEM account
        AccountResponse systemAccount = webClient.get()
                .uri(accountServiceUrl + "/system-account")
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();

        if (systemAccount == null) {
            System.out.println("SYSTEM account not found.");
            return;
        }

        // Fetch all active savings accounts
        List<AccountResponse> accounts = webClient.get()
                .uri(accountServiceUrl + "/accounts/savings/active")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AccountResponse>>() {})
                .block();

        if (accounts == null || accounts.isEmpty()) {
            System.out.println("No active savings accounts found.");
            return;
        }

        for (AccountResponse account : accounts) {

            BigDecimal interest = account.getBalance()
                    .multiply(BigDecimal.valueOf(0.05))
                    .setScale(2, RoundingMode.HALF_UP);

            TransferInitiationRequest initiationRequest =
                    new TransferInitiationRequest();

            initiationRequest.setFromAccountId(systemAccount.getAccountId());
            initiationRequest.setToAccountId(account.getAccountId());
            initiationRequest.setAmount(interest);
            initiationRequest.setDescription("Daily Interest");

            try {

                TransferInitiationResponse initiationResponse =
                        transactionService.initiateTransfer(initiationRequest);

                TransferExecutionRequest executionRequest =
                        new TransferExecutionRequest();

                executionRequest.setTransactionId(
                        initiationResponse.getTransactionId());

                transactionService.executeTransfer(executionRequest);

                System.out.println("Interest credited to account: "
                        + account.getAccountNumber());

            } catch (Exception ex) {

                System.out.println("Interest transfer failed for account "
                        + account.getAccountNumber());

                ex.printStackTrace();
            }
        }
    }
}