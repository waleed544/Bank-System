package com.example.logging_service.service;

import com.example.logging_service.dto.LogMessage;
import com.example.logging_service.entity.LoggingDump;
import com.example.logging_service.repo.LoggingDumpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingServiceImplTest {

    @Mock
    private LoggingDumpRepository repository;

    @InjectMocks
    private LoggingServiceImpl loggingService;

    @Test
    void save_mapsLogMessageFieldsOntoLoggingDump_andPersists() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 27, 10, 0);

        LogMessage message = LogMessage.builder()
                .service("account-service")
                .messageType("REQUEST")
                .message("PUT /accounts/transfer -> 200 OK")
                .timestamp(timestamp)
                .build();

        loggingService.save(message);

        ArgumentCaptor<LoggingDump> captor = ArgumentCaptor.forClass(LoggingDump.class);
        verify(repository).save(captor.capture());

        LoggingDump saved = captor.getValue();
        assertThat(saved.getMessage()).isEqualTo("PUT /accounts/transfer -> 200 OK");
        assertThat(saved.getMessageType()).isEqualTo("REQUEST");
        assertThat(saved.getDateTime()).isEqualTo(timestamp);
    }

    @Test
    void save_doesNotLoseMessageContent_forLongPayloads() {
        String longBody = "x".repeat(2000);

        LogMessage message = LogMessage.builder()
                .service("transaction-service")
                .messageType("RESPONSE")
                .message(longBody)
                .timestamp(LocalDateTime.now())
                .build();

        loggingService.save(message);

        ArgumentCaptor<LoggingDump> captor = ArgumentCaptor.forClass(LoggingDump.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getMessage()).hasSize(2000);
    }
}
