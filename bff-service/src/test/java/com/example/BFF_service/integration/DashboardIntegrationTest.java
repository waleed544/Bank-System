package com.example.BFF_service.integration;

import com.example.BFF_service.config.AccountServiceClient;
import com.example.BFF_service.config.TransactionServiceClient;
import com.example.BFF_service.config.UserServiceClient;
import com.example.BFF_service.dto.AccountResponse;
import com.example.BFF_service.dto.TransactionResponse;
import com.example.BFF_service.dto.UserProfileResponse;
import com.example.BFF_service.dto.AccountStatus;
import com.example.BFF_service.dto.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Full-stack integration test for the reactive BFF aggregation layer: real
 * Spring WebFlux context via WebTestClient, real Mono/Flux zipping logic in
 * DashboardServiceImpl. The three downstream clients (user, account,
 * transaction services) are mocked at the Spring bean level since BFF has no
 * database of its own — its only job is to aggregate calls to other services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DashboardIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    @MockitoBean
    private TransactionServiceClient transactionServiceClient;

    @Test
    void getDashboard_aggregatesUserAccountsAndTransactions() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        when(userServiceClient.getUser(userId)).thenReturn(Mono.just(
                UserProfileResponse.builder()
                        .userId(userId)
                        .username("hussein")
                        .email("hussein@example.com")
                        .firstName("Hussein")
                        .lastName("Hossam")
                        .build()));

        when(accountServiceClient.getAccounts(userId)).thenReturn(Flux.just(
                AccountResponse.builder()
                        .accountId(accountId)
                        .accountNumber("1000000001")
                        .accountType(AccountType.CHECKING)
                        .balance(new BigDecimal("500.00"))
                        .status(AccountStatus.ACTIVE)
                        .build()));

        when(transactionServiceClient.getTransactions(accountId)).thenReturn(Flux.just(
                TransactionResponse.builder()
                        .transactionId(UUID.randomUUID())
                        .fromAccountId(accountId)
                        .toAccountId(UUID.randomUUID())
                        .amount(new BigDecimal("-50.00"))
                        .description("Groceries")
                        .status("SUCCESS")
                        .deliveryStatus("DELIVERED")
                        .build()));

        webTestClient.get()
                .uri("/bff/dashboard/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("hussein")
                .jsonPath("$.accounts.length()").isEqualTo(1)
                .jsonPath("$.accounts[0].accountNumber").isEqualTo("1000000001")
                .jsonPath("$.accounts[0].transactions.length()").isEqualTo(1)
                .jsonPath("$.accounts[0].transactions[0].description").isEqualTo("Groceries");
    }

    @Test
    void getDashboard_returnsEmptyTransactions_whenUserHasNoAccounts() {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.getUser(userId)).thenReturn(Mono.just(
                UserProfileResponse.builder()
                        .userId(userId)
                        .username("newuser")
                        .email("newuser@example.com")
                        .firstName("New")
                        .lastName("User")
                        .build()));

        when(accountServiceClient.getAccounts(userId)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/bff/dashboard/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("newuser")
                .jsonPath("$.accounts.length()").isEqualTo(0);
    }

    @Test
    void getDashboard_returns500_whenUserServiceFails() {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.getUser(userId))
                .thenReturn(Mono.error(new RuntimeException("user-service unreachable")));
        when(accountServiceClient.getAccounts(any())).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/bff/dashboard/{userId}", userId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getDashboard_stillReturnsAccount_whenTransactionServiceHasNoDataForIt() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        when(userServiceClient.getUser(userId)).thenReturn(Mono.just(
                UserProfileResponse.builder()
                        .userId(userId)
                        .username("hussein")
                        .email("hussein@example.com")
                        .firstName("Hussein")
                        .lastName("Hossam")
                        .build()));

        when(accountServiceClient.getAccounts(userId)).thenReturn(Flux.just(
                AccountResponse.builder()
                        .accountId(accountId)
                        .accountNumber("1000000002")
                        .accountType(AccountType.SAVINGS)
                        .balance(new BigDecimal("1000.00"))
                        .status(AccountStatus.ACTIVE)
                        .build()));

        // Mirrors the real client's onErrorResume(-> Flux.empty()) behavior for a 404.
        when(transactionServiceClient.getTransactions(accountId)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/bff/dashboard/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accounts.length()").isEqualTo(1)
                .jsonPath("$.accounts[0].transactions.length()").isEqualTo(0);
    }
}
