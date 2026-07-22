package com.example.account_service.repositories;

import com.example.account_service.entities.accounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<accounts, UUID> {

    Optional<accounts> findByAccountNumber(String accountNumber);

    List<accounts> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);
}