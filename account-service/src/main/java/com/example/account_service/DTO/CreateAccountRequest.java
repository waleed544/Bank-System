package com.example.account_service.DTO;

import com.example.account_service.entities.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountRequest {

    private UUID userId;

    private AccountType accountType;

    private BigDecimal initialBalance;

}