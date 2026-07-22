package com.example.account_service.services;

import com.example.account_service.DTO.*;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    CreateAccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(UUID accountId);

    List<AccountResponse> getAccountsByUser(UUID userId);

    MessageResponse transfer(TransferRequest request);

}