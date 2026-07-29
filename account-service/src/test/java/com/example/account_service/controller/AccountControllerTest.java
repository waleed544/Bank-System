package com.example.account_service.controller;

import com.example.account_service.DTO.AccountResponse;
import com.example.account_service.DTO.CreateAccountResponse;
import com.example.account_service.DTO.MessageResponse;
import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.AccountType;
import com.example.account_service.services.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private com.example.account_service.kafka.LogProducer logProducer;

    // ---------- POST /accounts ----------

    @Test
    void createAccount_returns201_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        when(accountService.createAccount(any())).thenReturn(
                CreateAccountResponse.builder()
                        .accountId(accountId)
                        .accountNumber("1000000001")
                        .message("Account created successfully.")
                        .build());

        String body = """
                {
                  "userId": "%s",
                  "accountType": "CHECKING",
                  "initialBalance": 1000
                }
                """.formatted(userId);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("1000000001"));
    }

    @Test
    void createAccount_returns400_whenInitialBalanceMissing() throws Exception {
        String body = """
                {
                  "userId": "%s",
                  "accountType": "CHECKING"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.initialBalance").value("initialBalance is required"));
    }

    @Test
    void createAccount_returns400_whenInitialBalanceNegative() throws Exception {
        String body = """
                {
                  "userId": "%s",
                  "accountType": "CHECKING",
                  "initialBalance": -50
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.initialBalance").value("initialBalance cannot be negative"));
    }

    @Test
    void createAccount_returns400_whenUserIdAndAccountTypeMissing() throws Exception {
        String body = """
                {
                  "initialBalance": 500
                }
                """;

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.userId").value("userId is required"))
                .andExpect(jsonPath("$.message.accountType").value("accountType is required"));
    }

    // ---------- GET /accounts/{accountId} ----------

    @Test
    void getAccount_returns200_withAccountDetails() throws Exception {
        UUID accountId = UUID.randomUUID();

        when(accountService.getAccount(accountId)).thenReturn(
                AccountResponse.builder()
                        .accountId(accountId)
                        .accountNumber("1000000002")
                        .accountType(AccountType.SAVINGS)
                        .balance(new BigDecimal("250.00"))
                        .status(AccountStatus.ACTIVE)
                        .build());

        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000002"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---------- PUT /accounts/transfer ----------

    @Test
    void transfer_returns200_whenRequestIsValid() throws Exception {
        when(accountService.transfer(any())).thenReturn(
                MessageResponse.builder().message("Account updated successfully.").build());

        String body = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 100.00
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account updated successfully."));
    }

    @Test
    void transfer_returns400_whenAmountMissing() throws Exception {
        String body = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.amount").value("amount is required"));
    }

    @Test
    void transfer_returns400_whenAmountIsZeroOrNegative() throws Exception {
        String body = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 0
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.amount").value("amount must be greater than zero"));
    }
}
