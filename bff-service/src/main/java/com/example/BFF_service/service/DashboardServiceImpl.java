package com.example.BFF_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.BFF_service.config.AccountServiceClient;
import com.example.BFF_service.config.TransactionServiceClient;
import com.example.BFF_service.config.UserServiceClient;
import com.example.BFF_service.dto.AccountDto;
import com.example.BFF_service.dto.DashboardResponse;
import com.example.BFF_service.dto.TransactionDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserServiceClient userServiceClient;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;


    @Override
    public Mono<DashboardResponse> getDashboard(UUID userId) {

        // Step 1: fetch user profile
        Mono<com.example.BFF_service.dto.UserProfileResponse> userMono =
                userServiceClient.getUser(userId);

        // Step 2 + 3: fetch accounts, then for each account fetch transactions concurrently
        Mono<java.util.List<AccountDto>> accountsMono =
                accountServiceClient.getAccounts(userId)
                        // flatMap: for each account emit a Mono<AccountDto> that also fetches txns
                        .flatMap(account ->
                                transactionServiceClient
                                        .getTransactions(account.getAccountId())
                                        .map(tx -> TransactionDto.builder()
                                                .transactionId(tx.getTransactionId())
                                                .fromAccountId(tx.getFromAccountId())
                                                .toAccountId(tx.getToAccountId())
                                                .amount(tx.getAmount())
                                                .description(tx.getDescription())
                                                .timestamp(tx.getTimestamp())
                                                .build())
                                        .collectList()
                                        .map(transactions -> AccountDto.builder()
                                                .accountId(account.getAccountId())
                                                .accountNumber(account.getAccountNumber())
                                                .accountType(
                                                        account.getAccountType() != null
                                                                ? account.getAccountType().toString()
                                                                : null)
                                                .balance(account.getBalance())
                                                .status(
                                                        account.getStatus() != null
                                                                ? account.getStatus().toString()
                                                                : null)
                                                .transactions(transactions)
                                                .build())
                        )
                        .collectList();

        // Step 4: zip user + accounts into the aggregated dashboard response
        return Mono.zip(userMono, accountsMono)
                .map(tuple -> {
                    com.example.BFF_service.dto.UserProfileResponse user = tuple.getT1();
                    java.util.List<AccountDto> accounts = tuple.getT2();

                    return DashboardResponse.builder()
                            .userId(user.getUserId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .accounts(accounts)
                            .build();
                })
                .onErrorMap(ex ->
                        new com.example.BFF_service.exception.DownstreamServiceException(
                                "Failed to retrieve dashboard data due to an issue with downstream services.",
                                ex
                        )
                );
    }
}