package com.example.transaction_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferInitiationResponse {

    private UUID transactionId;

    private String status;

    private LocalDateTime timestamp;
}