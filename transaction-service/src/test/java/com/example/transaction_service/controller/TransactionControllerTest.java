package com.example.transaction_service.controller;

import com.example.transaction_service.dto.response.TransactionResponse;
import com.example.transaction_service.dto.response.TransferExecutionResponse;
import com.example.transaction_service.dto.response.TransferInitiationResponse;
import com.example.transaction_service.entities.TransactionStatus;
import com.example.transaction_service.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private com.example.transaction_service.kafka.LogProducer logProducer;

    // ---------- POST /transactions/transfer/initiation ----------

    @Test
    void initiateTransfer_returns200_whenRequestIsValid() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(transactionService.initiateTransfer(any())).thenReturn(
                TransferInitiationResponse.builder()
                        .transactionId(transactionId)
                        .status(TransactionStatus.INITIATED.name())
                        .build());

        String body = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 100.00,
                  "description": "rent"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/transactions/transfer/initiation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    void initiateTransfer_returns400_whenFromAccountIdMissing() throws Exception {
        String body = """
                {
                  "toAccountId": "%s",
                  "amount": 50
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/transactions/transfer/initiation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.fromAccountId").value("From account id is required"));
    }

    @Test
    void initiateTransfer_returns400_whenAmountIsZeroOrNegative() throws Exception {
        String body = """
                {
                  "fromAccountId": "%s",
                  "toAccountId": "%s",
                  "amount": 0
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/transactions/transfer/initiation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.amount").value("Amount must be greater than zero"));
    }

    // ---------- POST /transactions/transfer/execution ----------

    @Test
    void executeTransfer_returns200_whenRequestIsValid() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(transactionService.executeTransfer(any())).thenReturn(
                TransferExecutionResponse.builder()
                        .transactionId(transactionId)
                        .status(TransactionStatus.SUCCESS.name())
                        .build());

        String body = """
                {
                  "transactionId": "%s"
                }
                """.formatted(transactionId);

        mockMvc.perform(post("/transactions/transfer/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void executeTransfer_returns400_whenTransactionIdMissing() throws Exception {
        mockMvc.perform(post("/transactions/transfer/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.transactionId").value("Transaction id is required"));
    }

    // ---------- GET /accounts/{accountId}/transactions ----------

    @Test
    void getTransactions_returns200_withHistory() throws Exception {
        UUID accountId = UUID.randomUUID();

        when(transactionService.getTransactions(accountId)).thenReturn(
                List.of(TransactionResponse.builder()
                        .transactionId(UUID.randomUUID())
                        .fromAccountId(accountId)
                        .amount(new BigDecimal("-75.00"))
                        .description("groceries")
                        .build()));

        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(-75.00));
    }
}
