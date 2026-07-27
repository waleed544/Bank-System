package com.example.account_service.services;

import com.example.account_service.DTO.AccountResponse;
import com.example.account_service.DTO.CreateAccountRequest;
import com.example.account_service.DTO.CreateAccountResponse;
import com.example.account_service.DTO.MessageResponse;
import com.example.account_service.DTO.TransferRequest;
import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.AccountType;
import com.example.account_service.entities.accounts;
import com.example.account_service.exceptions.AccountInactiveException;
import com.example.account_service.exceptions.AccountNotFoundException;
import com.example.account_service.exceptions.InsufficientBalanceException;
import com.example.account_service.exceptions.InvalidAmountException;
import com.example.account_service.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository repository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID fromAccountId;
    private UUID toAccountId;
    private accounts fromAccount;
    private accounts toAccount;

    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();

        fromAccount = accounts.builder()
                .accountId(fromAccountId)
                .accountNumber("1000000001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        toAccount = accounts.builder()
                .accountId(toAccountId)
                .accountNumber("1000000002")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // ---------- createAccount() ----------

    @Test
    void createAccount_savesAndReturnsResponse_whenBalanceIsValid() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(UUID.randomUUID());
        request.setAccountType(AccountType.SAVINGS);
        request.setInitialBalance(new BigDecimal("250.00"));

        when(repository.existsByAccountNumber(any())).thenReturn(false);
        when(repository.save(any(accounts.class))).thenAnswer(invocation -> {
            accounts saved = invocation.getArgument(0);
            saved.setAccountId(UUID.randomUUID());
            return saved;
        });

        CreateAccountResponse response = accountService.createAccount(request);

        assertThat(response.getAccountId()).isNotNull();
        assertThat(response.getAccountNumber()).isNotBlank();
        assertThat(response.getMessage()).isEqualTo("Account created successfully.");
        verify(repository).save(any(accounts.class));
    }

    @Test
    void createAccount_throwsIllegalArgument_whenInitialBalanceIsNegative() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(UUID.randomUUID());
        request.setAccountType(AccountType.CHECKING);
        request.setInitialBalance(new BigDecimal("-10.00"));

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Initial balance cannot be negative.");

        verify(repository, never()).save(any());
    }

    // ---------- getAccount() ----------

    @Test
    void getAccount_returnsAccountResponse_whenFound() {
        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));

        AccountResponse response = accountService.getAccount(fromAccountId);

        assertThat(response.getAccountId()).isEqualTo(fromAccountId);
        assertThat(response.getBalance()).isEqualByComparingTo("500.00");
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void getAccount_throwsAccountNotFound_whenMissing() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(missingId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    // ---------- getAccountsByUser() ----------

    @Test
    void getAccountsByUser_returnsMappedList_whenAccountsExist() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(List.of(fromAccount, toAccount));

        List<AccountResponse> responses = accountService.getAccountsByUser(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAccountId()).isEqualTo(fromAccountId);
        assertThat(responses.get(1).getAccountId()).isEqualTo(toAccountId);
    }

    @Test
    void getAccountsByUser_throwsAccountNotFound_whenNoAccountsExist() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> accountService.getAccountsByUser(userId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("No accounts found.");
    }

    // ---------- transfer() ----------

    @Test
    void transfer_movesBalanceBetweenAccounts_whenRequestIsValid() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("150.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(repository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        when(repository.save(any(accounts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = accountService.transfer(request);

        assertThat(response.getMessage()).isEqualTo("Account updated successfully.");
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("350.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("250.00");
        verify(repository, times(2)).save(any(accounts.class));
    }

    @Test
    void transfer_throwsInvalidAmount_whenAmountIsZeroOrNegative() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must be greater than zero.");

        verify(repository, never()).findById(any());
    }

    @Test
    void transfer_throwsAccountNotFound_whenSourceAccountMissing() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("50.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Source account not found");
    }

    @Test
    void transfer_throwsAccountNotFound_whenDestinationAccountMissing() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("50.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(repository.findById(toAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Destination account not found");
    }

    @Test
    void transfer_throwsAccountInactive_whenSourceAccountIsInactive() {
        fromAccount.setStatus(AccountStatus.INACTIVE);

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("50.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(repository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessage("Source account is inactive.");
    }

    @Test
    void transfer_throwsIllegalArgument_whenTransferringToSameAccount() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(fromAccountId);
        request.setAmount(new BigDecimal("50.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot transfer to the same account.");
    }

    @Test
    void transfer_throwsAccountInactive_whenDestinationAccountIsInactive() {
        toAccount.setStatus(AccountStatus.INACTIVE);

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("50.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(repository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessage("Destination account is inactive.");
    }

    @Test
    void transfer_throwsInsufficientBalance_whenSourceLacksFunds() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal("999999.00"));

        when(repository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(repository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> accountService.transfer(request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance.");

        verify(repository, never()).save(any());
    }
}
