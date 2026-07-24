package com.example.transaction_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferExecutionRequest {

    @NotNull(message = "Transaction id is required")
    private UUID transactionId;
}