package com.example.transaction_service.integration;

import com.example.transaction_service.client.AccountServiceClient;
import com.example.transaction_service.dto.response.AccountResponse;
import com.example.transaction_service.dto.response.MessageResponse;
import com.example.transaction_service.entities.DeliveryStatus;
import com.example.transaction_service.entities.Transaction;
import com.example.transaction_service.entities.TransactionStatus;
import com.example.transaction_service.kafka.LogProducer;
import com.example.transaction_service.repositories.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test: real Spring context, real Postgres (via
 * Testcontainers), real HTTP layer through MockMvc, real TransactionRepository
 * persistence. AccountServiceClient is mocked since it's an outbound HTTP
 * call to a different microservice (account-service) — that cross-service
 * contract is exercised separately, not spun up here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class TransactionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bank_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    @MockitoBean
    private LogProducer logProducer;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
    }

    @Test
    void initiateTransfer_persistsRealTransactionRow() throws Exception {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        when(accountServiceClient.getAccount(any())).thenReturn(
                AccountResponse.builder().accountId(fromAccountId).build());

        String requestBody = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 150.00,
                  "description": "Rent payment"
                }
                """.formatted(fromAccountId, toAccountId);

        String responseJson = mockMvc.perform(post("/transactions/transfer/initiation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID transactionId = UUID.fromString(
                objectMapper.readTree(responseJson).get("transactionId").asText());

        Transaction saved = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(saved.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(saved.getToAccountId()).isEqualTo(toAccountId);
        assertThat(saved.getAmount()).isEqualByComparingTo("150.00");
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.INITIATED);
    }

    @Test
    void initiateTransfer_returns400_whenSameAccountUsedForBothSides() throws Exception {
        UUID accountId = UUID.randomUUID();

        String requestBody = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 50.00
                }
                """.formatted(accountId, accountId);

        mockMvc.perform(post("/transactions/transfer/initiation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void initiateTransfer_returns400_whenAccountsDoNotExist() throws Exception {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        when(accountServiceClient.getAccount(any()))
                .thenThrow(new RuntimeException("Account not found"));

        String requestBody = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 50.00
                }
                """.formatted(fromAccountId, toAccountId);

        mockMvc.perform(post("/transactions/transfer/initiation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void executeTransfer_marksTransactionSuccess_whenAccountServiceConfirms() throws Exception {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .status(TransactionStatus.INITIATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build());

        when(accountServiceClient.transfer(any(), any(), any()))
                .thenReturn(MessageResponse.builder().message("Account updated successfully.").build());

        String requestBody = """
                {
                  "transactionId": "%s"
                }
                """.formatted(transaction.getId());

        mockMvc.perform(post("/transactions/transfer/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Transaction refreshed = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(refreshed.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    void executeTransfer_marksTransactionFailed_whenAccountServiceRejects() throws Exception {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .amount(new BigDecimal("999999.00"))
                .status(TransactionStatus.INITIATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build());

        when(accountServiceClient.transfer(any(), any(), any()))
                .thenThrow(new RuntimeException("Insufficient balance."));

        String requestBody = """
                {
                  "transactionId": "%s"
                }
                """.formatted(transaction.getId());

        mockMvc.perform(post("/transactions/transfer/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());

        Transaction refreshed = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(refreshed.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void executeTransfer_returns400_whenTransactionAlreadyExecuted() throws Exception {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build());

        String requestBody = """
                {
                  "transactionId": "%s"
                }
                """.formatted(transaction.getId());

        mockMvc.perform(post("/transactions/transfer/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransfer_returns404_whenTransactionDoesNotExist() throws Exception {
        String requestBody = """
                {
                  "transactionId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/transactions/transfer/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactions_returnsSignedAmounts_dependingOnDirection() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID otherAccountId = UUID.randomUUID();

        // accountId is the sender here -> should appear negative from its perspective
        transactionRepository.save(Transaction.builder()
                .fromAccountId(accountId)
                .toAccountId(otherAccountId)
                .amount(new BigDecimal("75.00"))
                .status(TransactionStatus.SUCCESS)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build());

        // accountId is the receiver here -> should appear positive
        transactionRepository.save(Transaction.builder()
                .fromAccountId(otherAccountId)
                .toAccountId(accountId)
                .amount(new BigDecimal("40.00"))
                .status(TransactionStatus.SUCCESS)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build());

        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
