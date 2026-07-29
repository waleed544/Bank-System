package com.example.account_service.DTO;

import com.example.account_service.entities.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "accountType is required")
    private AccountType accountType;

    @NotNull(message = "initialBalance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "initialBalance cannot be negative")
    private BigDecimal initialBalance;
}