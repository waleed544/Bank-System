package com.example.account_service.config;

import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.AccountType;
import com.example.account_service.entities.accounts;
import com.example.account_service.repositories.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
public class SystemAccountConfig {

    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");


    @Bean
    CommandLineRunner createSystemAccount(AccountRepository accountRepository) {

        return args -> {

            if (!accountRepository.existsByAccountType(AccountType.SYSTEM)) {

                accounts systemAccount = accounts.builder()
                        .userId(SYSTEM_USER_ID)
                        .accountNumber("SYSTEM-INTEREST")
                        .accountType(AccountType.SYSTEM)
                        .balance(new BigDecimal("999999999999.99"))
                        .status(AccountStatus.ACTIVE)
                        .build();

                accountRepository.save(systemAccount);

                System.out.println("=====================================");
                System.out.println("SYSTEM INTEREST ACCOUNT CREATED");
                System.out.println("=====================================");
            }

        };
    }
}