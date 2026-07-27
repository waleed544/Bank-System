package com.example.transaction_service.service.impl;

import com.example.transaction_service.client.AccountServiceClient;
import com.example.transaction_service.dto.request.TransferExecutionRequest;
import com.example.transaction_service.dto.request.TransferInitiationRequest;
import com.example.transaction_service.dto.response.MessageResponse;
import com.example.transaction_service.dto.response.TransactionResponse;
import com.example.transaction_service.dto.response.TransferExecutionResponse;
import com.example.transaction_service.dto.response.TransferInitiationResponse;
import com.example.transaction_service.entities.DeliveryStatus;
import com.example.transaction_service.entities.Transaction;
import com.example.transaction_service.entities.TransactionStatus;
import com.example.transaction_service.exception.AccountValidationException;
import com.example.transaction_service.exception.InvalidTransactionStateException;
import com.example.transaction_service.exception.TransactionNotFoundException;
import com.example.transaction_service.exception.TransferExecutionException;
import com.example.transaction_service.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private UUID fromAccountId;
    private UUID toAccountId;

    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
    }

    // ---------- initiateTransfer() ----------

    @Test
    void initiateTransfer_savesTransaction_whenBothAccountsExist() {
        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("rent");

        UUID generatedId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction toSave = invocation.getArgument(0);
            toSave.setTransactionId(generatedId);
            toSave.setCreatedAt(now);
            return toSave;
        });

        TransferInitiationResponse response = transactionService.initiateTransfer(request);

        assertThat(response.getTransactionId()).isEqualTo(generatedId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.INITIATED.name());

        verify(accountServiceClient).getAccount(fromAccountId);
        verify(accountServiceClient).getAccount(toAccountId);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void initiateTransfer_throwsAccountValidation_whenSameAccount() {
        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(fromAccountId);
        request.setAmount(new BigDecimal("50.00"));

        assertThatThrownBy(() -> transactionService.initiateTransfer(request))
                .isInstanceOf(AccountValidationException.class)
                .hasMessage("Source and destination accounts cannot be the same.");

        verify(accountServiceClient, never()).getAccount(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void initiateTransfer_throwsAccountValidation_whenAccountLookupFails() {
        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("50.00"));

        when(accountServiceClient.getAccount(fromAccountId))
                .thenThrow(new RuntimeException("404 not found"));

        assertThatThrownBy(() -> transactionService.initiateTransfer(request))
                .isInstanceOf(AccountValidationException.class)
                .hasMessage("One or both accounts do not exist.");

        verify(transactionRepository, never()).save(any());
    }

    // ---------- executeTransfer() ----------

    @Test
    void executeTransfer_marksSuccess_whenAccountServiceCallSucceeds() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.INITIATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();

        TransferExecutionRequest request = new TransferExecutionRequest();
        request.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(accountServiceClient.transfer(fromAccountId, toAccountId, transaction.getAmount()))
                .thenReturn(MessageResponse.builder().message("Account updated successfully.").build());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferExecutionResponse response = transactionService.executeTransfer(request);

        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(transaction.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void executeTransfer_throwsTransactionNotFound_whenTransactionMissing() {
        UUID transactionId = UUID.randomUUID();
        TransferExecutionRequest request = new TransferExecutionRequest();
        request.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.executeTransfer(request))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction not found");

        verify(accountServiceClient, never()).transfer(any(), any(), any());
    }

    @Test
    void executeTransfer_throwsInvalidState_whenTransactionAlreadyExecuted() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        TransferExecutionRequest request = new TransferExecutionRequest();
        request.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.executeTransfer(request))
                .isInstanceOf(InvalidTransactionStateException.class)
                .hasMessage("Only initiated transactions can be executed.");

        verify(accountServiceClient, never()).transfer(any(), any(), any());
    }

    @Test
    void executeTransfer_marksFailed_whenAccountServiceReturnsErrorResponse() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("999999.00"))
                .status(TransactionStatus.INITIATED)
                .build();

        TransferExecutionRequest request = new TransferExecutionRequest();
        request.setTransactionId(transactionId);

        WebClientResponseException webClientEx = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Insufficient balance.",
                HttpHeaders.EMPTY,
                new byte[0],
                null);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(accountServiceClient.transfer(fromAccountId, toAccountId, transaction.getAmount()))
                .thenThrow(webClientEx);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> transactionService.executeTransfer(request))
                .isInstanceOf(TransferExecutionException.class);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(transaction.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void executeTransfer_marksFailed_whenAccountServiceUnreachable() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.INITIATED)
                .build();

        TransferExecutionRequest request = new TransferExecutionRequest();
        request.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(accountServiceClient.transfer(fromAccountId, toAccountId, transaction.getAmount()))
                .thenThrow(new RuntimeException("Connection refused"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> transactionService.executeTransfer(request))
                .isInstanceOf(TransferExecutionException.class)
                .hasMessage("Connection refused");

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    // ---------- getTransactions() ----------

    @Test
    void getTransactions_negatesAmount_forOutgoingTransaction() {
        Transaction outgoing = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("75.00"))
                .status(TransactionStatus.SUCCESS)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(transactionRepository.findByFromAccountIdOrToAccountId(fromAccountId, fromAccountId))
                .thenReturn(List.of(outgoing));

        List<TransactionResponse> responses = transactionService.getTransactions(fromAccountId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAmount()).isEqualByComparingTo("-75.00");
    }

    @Test
    void getTransactions_keepsAmountPositive_forIncomingTransaction() {
        Transaction incoming = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("75.00"))
                .status(TransactionStatus.SUCCESS)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(transactionRepository.findByFromAccountIdOrToAccountId(toAccountId, toAccountId))
                .thenReturn(List.of(incoming));

        List<TransactionResponse> responses = transactionService.getTransactions(toAccountId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAmount()).isEqualByComparingTo("75.00");
    }
}
