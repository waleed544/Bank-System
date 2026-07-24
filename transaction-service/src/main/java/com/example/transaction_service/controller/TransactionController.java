package com.example.transaction_service.controller;

import com.example.transaction_service.dto.request.TransferExecutionRequest;
import com.example.transaction_service.dto.request.TransferInitiationRequest;
import com.example.transaction_service.dto.response.TransactionResponse;
import com.example.transaction_service.dto.response.TransferExecutionResponse;
import com.example.transaction_service.dto.response.TransferInitiationResponse;
import com.example.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transactions/transfer/initiation")
    public ResponseEntity<TransferInitiationResponse> initiateTransfer(
            @Valid @RequestBody TransferInitiationRequest request) {

        return ResponseEntity.ok(
                transactionService.initiateTransfer(request));
    }

    @PostMapping("/transactions/transfer/execution")
    public ResponseEntity<TransferExecutionResponse> executeTransfer(
            @Valid @RequestBody TransferExecutionRequest request) {

        return ResponseEntity.ok(
                transactionService.executeTransfer(request)
        );
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(
                transactionService.getTransactions(accountId)
        );
    }
}