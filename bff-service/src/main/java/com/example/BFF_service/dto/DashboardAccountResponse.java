package com.example.BFF_service.dto;

import com.example.BFF_service.dto.TransactionResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAccountResponse {

    private UUID accountId;

    private String accountNumber;

    private String accountType;

    private BigDecimal balance;

    private String status;

    private List<TransactionResponse> transactions;

}
