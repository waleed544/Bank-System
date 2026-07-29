package com.example.account_service.integration;

import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.AccountType;
import com.example.account_service.entities.accounts;
import com.example.account_service.kafka.LogProducer;
import com.example.account_service.repositories.AccountRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test: real Spring context, real Postgres (via
 * Testcontainers), real HTTP layer through MockMvc. Kafka's LogProducer is
 * mocked since we're verifying the REST + DB flow here, not the logging
 * pipeline (that's covered separately by LogProducerTest / logging-service's
 * own consumer tests).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,properties = {"spring.kafka.bootstrap-servers=localhost:9092"})

@AutoConfigureMockMvc
@Testcontainers
class AccountIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("bank_test").withUsername("test").withPassword("test").withReuse(false);

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
    private AccountRepository accountRepository;

    // Kafka logging is a side effect we don't need a real broker for here.
    @MockitoBean
    private LogProducer logProducer;

    @BeforeEach
    void cleanDatabase() {
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_persistsRealRowInDatabase() throws Exception {
        UUID userId = UUID.randomUUID();

        String requestBody = """
                {
                  "userId": "%s",
                  "accountType": "CHECKING",
                  "initialBalance": 1000
                }
                """.formatted(userId);

        String responseJson = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID accountId = UUID.fromString(
                objectMapper.readTree(responseJson).get("accountId").asText());

        // The real assertion: did it actually land in Postgres, not just the HTTP response?
        Optional<accounts> saved = accountRepository.findById(accountId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getUserId()).isEqualTo(userId);
        assertThat(saved.get().getBalance()).isEqualByComparingTo("1000");
        assertThat(saved.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void getAccount_returnsRealPersistedAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        accounts account = accountRepository.save(accounts.builder()
                .userId(userId)
                .accountNumber("9000000001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/accounts/{accountId}", account.getAccountId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("9000000001"))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void getAccounts_returnsAllAccountsForUser() throws Exception {
        UUID userId = UUID.randomUUID();
        accountRepository.save(accounts.builder()
                .userId(userId).accountNumber("9000000002")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).build());
        accountRepository.save(accounts.builder()
                .userId(userId).accountNumber("9000000003")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("200.00")).status(AccountStatus.ACTIVE).build());

        mockMvc.perform(get("/users/{userId}/accounts", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void transfer_movesRealMoneyBetweenPersistedAccounts() throws Exception {
        UUID userId = UUID.randomUUID();
        accounts from = accountRepository.save(accounts.builder()
                .userId(userId).accountNumber("9000000004")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00")).status(AccountStatus.ACTIVE).build());
        accounts to = accountRepository.save(accounts.builder()
                .userId(userId).accountNumber("9000000005")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).build());

        String requestBody = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 150.00
                }
                """.formatted(from.getAccountId(), to.getAccountId());

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account updated successfully."));

        // Re-read from the real DB — confirms the transaction actually committed correctly.
        accounts refreshedFrom = accountRepository.findById(from.getAccountId()).orElseThrow();
        accounts refreshedTo = accountRepository.findById(to.getAccountId()).orElseThrow();

        assertThat(refreshedFrom.getBalance()).isEqualByComparingTo("350.00");
        assertThat(refreshedTo.getBalance()).isEqualByComparingTo("250.00");
    }

    @Test
    void transfer_returns400_andDoesNotTouchDatabase_whenAmountInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        accounts from = accountRepository.save(accounts.builder()
                .userId(userId).accountNumber("9000000006")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00")).status(AccountStatus.ACTIVE).build());
        accounts to = accountRepository.save(accounts.builder()
                .userId(userId).accountNumber("9000000007")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).build());

        String requestBody = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 0
                }
                """.formatted(from.getAccountId(), to.getAccountId());

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Balances must be untouched since validation rejected the request before any transfer logic ran.
        accounts unchangedFrom = accountRepository.findById(from.getAccountId()).orElseThrow();
        accounts unchangedTo = accountRepository.findById(to.getAccountId()).orElseThrow();
        assertThat(unchangedFrom.getBalance()).isEqualByComparingTo("500.00");
        assertThat(unchangedTo.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void getAccounts_returns404_whenUserHasNoAccounts() throws Exception {
        mockMvc.perform(get("/users/{userId}/accounts", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
