package com.example.transaction_service.dto.response;

import com.example.transaction_service.entities.AccountType;
import com.example.transaction_service.entities.AccountStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
public class AccountResponse {

    private UUID accountId;

    private String accountNumber;

    private AccountType accountType;

    private BigDecimal balance;

    private AccountStatus status;

}
