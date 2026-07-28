package com.example.account_service.kafka;

import com.example.account_service.DTO.LogMessage;
import com.example.account_service.config.KafkaTopicConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogProducerTest {

    @Mock
    private KafkaTemplate<String, LogMessage> kafkaTemplate;

    @InjectMocks
    private LogProducer logProducer;

    @Test
    void send_publishesToBankLogsTopic_withAllFieldsPopulated() {
        logProducer.send("account-service", "REQUEST", "PUT /accounts/transfer -> 200 OK");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LogMessage> messageCaptor = ArgumentCaptor.forClass(LogMessage.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopicConfig.TOPIC);

        LogMessage sent = messageCaptor.getValue();
        assertThat(sent.getService()).isEqualTo("account-service");
        assertThat(sent.getMessageType()).isEqualTo("REQUEST");
        assertThat(sent.getMessage()).isEqualTo("PUT /accounts/transfer -> 200 OK");
        assertThat(sent.getTimestamp()).isNotNull();
    }

    @Test
    void send_setsTimestampCloseToNow() {
        var before = java.time.LocalDateTime.now().minusSeconds(1);

        logProducer.send("account-service", "RESPONSE", "some message");

        ArgumentCaptor<LogMessage> messageCaptor = ArgumentCaptor.forClass(LogMessage.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.anyString(), messageCaptor.capture());

        var after = java.time.LocalDateTime.now().plusSeconds(1);
        assertThat(messageCaptor.getValue().getTimestamp()).isBetween(before, after);
    }
}
