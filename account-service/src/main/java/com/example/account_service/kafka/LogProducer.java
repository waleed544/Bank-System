package com.example.account_service.kafka;

import com.example.account_service.config.KafkaTopicConfig;
import com.example.account_service.DTO.LogMessage;
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