package com.example.account_service.DTO;

import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.AccountType;
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