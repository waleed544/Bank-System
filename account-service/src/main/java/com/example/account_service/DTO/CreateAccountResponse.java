package com.example.account_service.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class CreateAccountResponse {

    private UUID accountId;

    private String accountNumber;

    private String message;

}
