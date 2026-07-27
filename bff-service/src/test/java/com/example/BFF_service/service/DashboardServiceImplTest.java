package com.example.BFF_service.service;

import com.example.BFF_service.config.AccountServiceClient;
import com.example.BFF_service.config.TransactionServiceClient;
import com.example.BFF_service.config.UserServiceClient;
import com.example.BFF_service.dto.AccountResponse;
import com.example.BFF_service.dto.AccountStatus;
import com.example.BFF_service.dto.AccountType;
import com.example.BFF_service.dto.DashboardResponse;
import com.example.BFF_service.dto.TransactionResponse;
import com.example.BFF_service.dto.UserProfileResponse;
import com.example.BFF_service.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void getDashboard_aggregatesUserAccountsAndTransactions_whenAllDownstreamCallsSucceed() {
        UserProfileResponse user = UserProfileResponse.builder()
                .userId(userId)
                .username("hussein")
                .email("hussein@example.com")
                .firstName("Hussein")
                .lastName("Hossam")
                .build();

        AccountResponse account = AccountResponse.builder()
                .accountId(accountId)
                .accountNumber("1000000001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        TransactionResponse transaction = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .fromAccountId(accountId)
                .amount(new BigDecimal("-50.00"))
                .description("groceries")
                .build();

        when(userServiceClient.getUser(userId)).thenReturn(Mono.just(user));
        when(accountServiceClient.getAccounts(userId)).thenReturn(Flux.just(account));
        when(transactionServiceClient.getTransactions(accountId)).thenReturn(Flux.just(transaction));

        StepVerifier.create(dashboardService.getDashboard(userId))
                .assertNext(dashboard -> {
                    org.assertj.core.api.Assertions.assertThat(dashboard.getUserId()).isEqualTo(userId);
                    org.assertj.core.api.Assertions.assertThat(dashboard.getUsername()).isEqualTo("hussein");
                    org.assertj.core.api.Assertions.assertThat(dashboard.getAccounts()).hasSize(1);
                    org.assertj.core.api.Assertions.assertThat(dashboard.getAccounts().get(0).getAccountNumber())
                            .isEqualTo("1000000001");
                    org.assertj.core.api.Assertions.assertThat(dashboard.getAccounts().get(0).getTransactions())
                            .hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getDashboard_returnsAccountWithEmptyTransactions_whenAccountHasNoHistory() {
        UserProfileResponse user = UserProfileResponse.builder()
                .userId(userId)
                .username("hussein")
                .build();

        AccountResponse account = AccountResponse.builder()
                .accountId(accountId)
                .accountNumber("1000000002")
                .accountType(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userServiceClient.getUser(userId)).thenReturn(Mono.just(user));
        when(accountServiceClient.getAccounts(userId)).thenReturn(Flux.just(account));
        when(transactionServiceClient.getTransactions(accountId)).thenReturn(Flux.empty());

        StepVerifier.create(dashboardService.getDashboard(userId))
                .assertNext(dashboard ->
                        org.assertj.core.api.Assertions.assertThat(dashboard.getAccounts().get(0).getTransactions())
                                .isEmpty())
                .verifyComplete();
    }

    @Test
    void getDashboard_wrapsFailureInDownstreamServiceException_whenUserServiceFails() {
        when(userServiceClient.getUser(userId))
                .thenReturn(Mono.error(new RuntimeException("user-service unreachable")));
        when(accountServiceClient.getAccounts(userId)).thenReturn(Flux.empty());

        StepVerifier.create(dashboardService.getDashboard(userId))
                .expectErrorMatches(ex ->
                        ex instanceof DownstreamServiceException
                                && ex.getMessage().equals(
                                "Failed to retrieve dashboard data due to an issue with downstream services."))
                .verify();
    }

    @Test
    void getDashboard_wrapsFailureInDownstreamServiceException_whenAccountServiceFails() {
        UserProfileResponse user = UserProfileResponse.builder().userId(userId).username("hussein").build();

        when(userServiceClient.getUser(userId)).thenReturn(Mono.just(user));
        when(accountServiceClient.getAccounts(userId))
                .thenReturn(Flux.error(new RuntimeException("account-service unreachable")));

        StepVerifier.create(dashboardService.getDashboard(userId))
                .expectError(DownstreamServiceException.class)
                .verify();
    }
}
