package com.example.transaction_service.service;

import com.example.transaction_service.dto.request.TransferExecutionRequest;
import com.example.transaction_service.dto.request.TransferInitiationRequest;
import com.example.transaction_service.dto.response.TransactionResponse;
import com.example.transaction_service.dto.response.TransferExecutionResponse;
import com.example.transaction_service.dto.response.TransferInitiationResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransferInitiationResponse initiateTransfer(TransferInitiationRequest request);

    TransferExecutionResponse executeTransfer(TransferExecutionRequest request);

    List<TransactionResponse> getTransactions(UUID accountId);

}