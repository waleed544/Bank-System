package com.example.account_service.scheduler;

import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.accounts;
import com.example.account_service.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleAccountScheduler {

    private final AccountRepository repository;

    @Scheduled(fixedRate = 3_600_000)
//    @Scheduled(fixedRate = 10000)
    public void deactivateInactiveAccounts() {

          LocalDateTime threshold = LocalDateTime.now().minusHours(24);
//        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);

        List<accounts> staleAccounts =
                repository.findByStatusAndLastTransactionAtBefore(
                        AccountStatus.ACTIVE,
                        threshold
                );

        staleAccounts.forEach(account ->
                account.setStatus(AccountStatus.INACTIVE));

        repository.saveAll(staleAccounts);

        log.info("Scheduler: {} accounts marked as INACTIVE.",
                staleAccounts.size());
    }
}