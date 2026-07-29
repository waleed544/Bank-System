package com.example.account_service.scheduler;

import com.example.account_service.entities.AccountStatus;
import com.example.account_service.entities.accounts;
import com.example.account_service.repositories.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleAccountSchedulerTest {

    @Mock
    private AccountRepository repository;

    @InjectMocks
    private StaleAccountScheduler scheduler;

    @Test
    void deactivateInactiveAccounts_marksStaleAccountsInactive_andSavesThem() {
        accounts staleAccountOne = accounts.builder()
                .status(AccountStatus.ACTIVE)
                .lastTransactionAt(LocalDateTime.now().minusHours(48))
                .build();

        accounts staleAccountTwo = accounts.builder()
                .status(AccountStatus.ACTIVE)
                .lastTransactionAt(LocalDateTime.now().minusHours(30))
                .build();

        when(repository.findByStatusAndLastTransactionAtBefore(eq(AccountStatus.ACTIVE), any()))
                .thenReturn(List.of(staleAccountOne, staleAccountTwo));

        scheduler.deactivateInactiveAccounts();

        assertThat(staleAccountOne.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(staleAccountTwo.getStatus()).isEqualTo(AccountStatus.INACTIVE);

        ArgumentCaptor<List<accounts>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void deactivateInactiveAccounts_savesEmptyList_whenNoStaleAccountsFound() {
        when(repository.findByStatusAndLastTransactionAtBefore(eq(AccountStatus.ACTIVE), any()))
                .thenReturn(List.of());

        scheduler.deactivateInactiveAccounts();

        verify(repository).saveAll(List.of());
    }

    @Test
    void deactivateInactiveAccounts_neverTouchesAlreadyInactiveAccounts() {
        // The repository query itself filters by ACTIVE status, so an already-INACTIVE
        // account should never even be passed to this method's save step.
        when(repository.findByStatusAndLastTransactionAtBefore(eq(AccountStatus.ACTIVE), any()))
                .thenReturn(List.of());

        scheduler.deactivateInactiveAccounts();

        verify(repository, never()).save(any());
    }
}
