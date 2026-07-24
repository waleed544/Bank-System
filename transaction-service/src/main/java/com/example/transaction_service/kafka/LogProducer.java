package com.example.transaction_service.kafka;

import com.example.transaction_service.dto.LogMessage;
import com.example.transaction_service.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LogProducer {

    private final KafkaTemplate<String, LogMessage> kafkaTemplate;

    public void send(String service,
                     String messageType,
                     String message) {

        LogMessage log = LogMessage.builder()
                .service(service)
                .messageType(messageType)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.TOPIC, log);

    }

}