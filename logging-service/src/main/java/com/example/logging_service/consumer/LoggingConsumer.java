package com.example.logging_service.consumer;

import com.example.logging_service.dto.LogMessage;
import com.example.logging_service.service.LoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoggingConsumer {

    private final LoggingService loggingService;

    @KafkaListener(
            topics = "bank-logs",
            groupId = "logging-group")
    public void consume(LogMessage message) {

        loggingService.save(message);

        System.out.println("Kafka Log Received: " + message.getMessage());

    }

}