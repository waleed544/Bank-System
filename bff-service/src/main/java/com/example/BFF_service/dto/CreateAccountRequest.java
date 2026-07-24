package com.example.BFF_service.dto;

import com.example.BFF_service.dto.AccountType;
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
