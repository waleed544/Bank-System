package com.example.logging_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC = "bank-logs";

    @Bean
    public NewTopic bankLogsTopic() {
        return new NewTopic(TOPIC, 1, (short) 1);
    }
}