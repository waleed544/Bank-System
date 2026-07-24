package com.example.transaction_service.dto.request;



import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class AccountTransferRequest {

    private UUID fromAccountId;

    private UUID toAccountId;

    private BigDecimal amount;

}