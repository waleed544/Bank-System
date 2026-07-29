package com.example.account_service.services;

import com.example.account_service.DTO.*;
import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.AccountType;
import com.example.account_service.entities.accounts;
import com.example.account_service.exceptions.AccountInactiveException;
import com.example.account_service.exceptions.AccountNotFoundException;
import com.example.account_service.exceptions.InsufficientBalanceException;
import com.example.account_service.exceptions.InvalidAmountException;
import com.example.account_service.repositories.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;

    public AccountServiceImpl(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        accounts account = new accounts();

        account.setUserId(request.getUserId());

        account.setAccountType(request.getAccountType());

        account.setBalance(request.getInitialBalance());

        account.setStatus(AccountStatus.ACTIVE);

        account.setLastTransactionAt(LocalDateTime.now());

        account.setAccountNumber(generateAccountNumber());

        account = repository.save(account);

        return CreateAccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .message("Account created successfully.")
                .build();
    }

    @Override
    public AccountResponse getAccount(UUID accountId) {

        accounts account = repository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account not found"));

        return map(account);
    }


    @Override
    public List<AccountResponse> getAccountsByUser(UUID userId) {

        List<accounts> accounts = repository.findByUserId(userId);

        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found.");
        }

        return accounts.stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse transfer(TransferRequest request) {

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }

        accounts from = repository.findById(request.getFromAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException("Source account not found"));

        accounts to = repository.findById(request.getToAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException("Destination account not found"));

        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Source account is inactive.");
        }
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Destination account is inactive.");
        }

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance.");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));

        to.setBalance(to.getBalance().add(request.getAmount()));

        from.setLastTransactionAt(LocalDateTime.now());

        to.setLastTransactionAt(LocalDateTime.now());

        repository.save(from);

        repository.save(to);

        return MessageResponse.builder()
                .message("Account updated successfully.")
                .build();
    }

    private AccountResponse map(accounts account) {

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    private String generateAccountNumber() {

        Random random = new Random();

        String accountNumber;

        do {
            accountNumber = String.valueOf(
                    1000000000L +
                            Math.abs(random.nextLong()) % 9000000000L
            );
        } while (repository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
    public List<accounts> getActiveSavingsAccounts() {

        return repository.findByAccountTypeAndStatus(
                AccountType.SAVINGS,
                AccountStatus.ACTIVE
        );

    }

    @Override
    public AccountResponse getSystemAccount() {

        accounts account = repository.findByAccountType(AccountType.SYSTEM)
                .orElseThrow(() -> new RuntimeException("System account not found"));

        return map(account);
    }



}
