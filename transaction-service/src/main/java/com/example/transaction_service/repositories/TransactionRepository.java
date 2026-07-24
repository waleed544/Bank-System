package com.example.transaction_service.repositories;

import com.example.transaction_service.entities.Transaction;
import com.example.transaction_service.entities.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByFromAccountIdOrToAccountId(UUID fromAccountId, UUID toAccountId);

    Optional<Transaction> findByIdAndStatus(UUID id, TransactionStatus status);
}