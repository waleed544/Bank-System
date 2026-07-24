package com.example.transaction_service.service.impl;
import com.example.transaction_service.client.AccountServiceClient;
import com.example.transaction_service.dto.request.TransferExecutionRequest;
import com.example.transaction_service.dto.request.TransferInitiationRequest;
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
import com.example.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    @Override
    public TransferInitiationResponse initiateTransfer(TransferInitiationRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new AccountValidationException(
                    "Source and destination accounts cannot be the same.");
        }
        try {
            accountServiceClient.getAccount(request.getFromAccountId());
            accountServiceClient.getAccount(request.getToAccountId());
        } catch (Exception ex) {
            throw new AccountValidationException(
                    "One or both accounts do not exist.");
        }

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(TransactionStatus.INITIATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        return TransferInitiationResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus().name())
                .timestamp(transaction.getTimestamp())
                .build();
    }

    @Override
    public TransferExecutionResponse executeTransfer(TransferExecutionRequest request) {

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new InvalidTransactionStateException(
                    "Only initiated transactions can be executed.");
        }
        try {

            accountServiceClient.transfer(
                    transaction.getFromAccountId(),
                    transaction.getToAccountId(),
                    transaction.getAmount()
            );

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setDeliveryStatus(DeliveryStatus.DELIVERED);

        } catch (WebClientResponseException ex) {

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDeliveryStatus(DeliveryStatus.FAILED);

            transactionRepository.save(transaction);

            throw new TransferExecutionException(
                    ex.getResponseBodyAsString(), ex);
        }
        catch (Exception ex) {

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDeliveryStatus(DeliveryStatus.FAILED);

            transactionRepository.save(transaction);

            throw new TransferExecutionException(
                    ex.getMessage(), ex);
        }


        transactionRepository.save(transaction);

        return TransferExecutionResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<TransactionResponse> getTransactions(UUID accountId) {

        return transactionRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId)
                .stream()
                .map(transaction -> TransactionResponse.builder()
                        .transactionId(transaction.getId())
                        .fromAccountId(transaction.getFromAccountId())
                        .toAccountId(transaction.getToAccountId())
                        .amount(
                                transaction.getFromAccountId().equals(accountId)
                                        ? transaction.getAmount().negate()
                                        : transaction.getAmount()
                        )
                        .description(transaction.getDescription())
                        .status(transaction.getStatus().name())
                        .deliveryStatus(transaction.getDeliveryStatus().name())
                        .timestamp(transaction.getTimestamp())
                        .build())
                .toList();
    }
}