package com.example.logging_service.consumer;

import com.example.logging_service.dto.LogMessage;
import com.example.logging_service.service.LoggingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingConsumerTest {

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private LoggingConsumer loggingConsumer;

    @Test
    void consume_delegatesReceivedMessageToLoggingService() {
        LogMessage message = LogMessage.builder()
                .service("transaction-service")
                .messageType("REQUEST")
                .message("POST /transactions/transfer/initiation -> 200 OK")
                .timestamp(LocalDateTime.now())
                .build();

        loggingConsumer.consume(message);

        verify(loggingService).save(message);
    }
}
