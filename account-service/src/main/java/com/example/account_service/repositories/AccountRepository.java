package com.example.account_service.repositories;

import com.example.account_service.entities.AccountType;
import com.example.account_service.entities.accounts;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.accounts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;


public interface AccountRepository extends JpaRepository<accounts, UUID> {

    Optional<accounts> findByAccountType(AccountType accountType);
    Optional<accounts> findByAccountNumber(String accountNumber);

    List<accounts> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);
    List<accounts> findByStatusAndLastTransactionAtBefore(
            AccountStatus status,
            LocalDateTime dateTime
    );
    boolean existsByAccountType(AccountType accountType);
    List<accounts> findByAccountTypeAndStatus(
            AccountType accountType,
            AccountStatus status
    );
}