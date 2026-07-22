package com.example.account_service.controller;

import com.example.account_service.DTO.AccountResponse;
import com.example.account_service.DTO.CreateAccountRequest;
import com.example.account_service.DTO.CreateAccountResponse;
import com.example.account_service.DTO.*;
import com.example.account_service.services.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<CreateAccountResponse> createAccount(
            @RequestBody CreateAccountRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(
                accountService.getAccount(accountId)
        );
    }

    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                accountService.getAccountsByUser(userId)
        );
    }

    @PutMapping("/accounts/transfer")
    public ResponseEntity<MessageResponse> transfer(
            @RequestBody TransferRequest request) {

        return ResponseEntity.ok(
                accountService.transfer(request)
        );
    }

}