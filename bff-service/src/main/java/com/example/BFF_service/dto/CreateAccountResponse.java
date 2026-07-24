package com.example.BFF_service.dto;

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

