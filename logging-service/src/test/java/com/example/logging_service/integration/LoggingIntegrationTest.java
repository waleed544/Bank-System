package com.example.logging_service.integration;

import com.example.logging_service.dto.LogMessage;
import com.example.logging_service.entity.LoggingDump;
import com.example.logging_service.repo.LoggingDumpRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full-stack integration test for logging-service: real Postgres + real
 * Kafka broker via Testcontainers. A message is produced onto the actual
 * "bank-logs" topic (the same way account-service/user-service/etc. do it
 * in production, via a JsonSerializer-based KafkaTemplate), the real
 * @KafkaListener (LoggingConsumer) consumes it, and we assert the row
 * actually lands in Postgres — not mocking LoggingService or the consumer.
 *
 * Note: LoggingDumpRepository is declared as JpaRepository<LoggingDump, UUID>
 * even though LoggingDump's real @Id is a Long (auto-generated identity).
 * That's an existing mismatch in the production code (flagged separately,
 * not fixed here). To avoid exercising that bug, this test never calls
 * findById(...) with a UUID; it reads back via findAll() and matches on
 * message content instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class LoggingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bank_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    private static KafkaTemplate<String, LogMessage> testProducer;

    @org.springframework.beans.factory.annotation.Autowired
    private LoggingDumpRepository repository;

    @BeforeAll
    static void startProducer() {
        // Mirrors the real producer config used by account-service /
        // user-service / transaction-service's KafkaProducerConfig: String
        // key, JsonSerializer value, type-info headers disabled (the
        // consumer relies on JsonDeserializer.VALUE_DEFAULT_TYPE instead).
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        ProducerFactory<String, LogMessage> producerFactory = new DefaultKafkaProducerFactory<>(config);
        testProducer = new KafkaTemplate<>(producerFactory);
    }

    @AfterAll
    static void stopProducer() {
        if (testProducer != null) {
            testProducer.destroy();
        }
    }

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void consume_realKafkaMessage_getsPersistedToPostgres() {
        LocalDateTime timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String uniqueBody = "PUT /accounts/transfer -> 200 OK [" + UUID.randomUUID() + "]";

        LogMessage message = LogMessage.builder()
                .service("account-service")
                .messageType("REQUEST")
                .message(uniqueBody)
                .timestamp(timestamp)
                .build();

        testProducer.send("bank-logs", message);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            List<LoggingDump> rows = repository.findAll();
            List<LoggingDump> matches = rows.stream()
                    .filter(row -> uniqueBody.equals(row.getMessage()))
                    .toList();

            assertThat(matches).hasSize(1);

            LoggingDump saved = matches.get(0);
            assertThat(saved.getMessageType()).isEqualTo("REQUEST");
            assertThat(saved.getDateTime()).isEqualTo(timestamp);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getId()).isNotNull();
        });
    }

    @Test
    void consume_multipleRealMessages_persistsEachIndependently() {
        String runId = UUID.randomUUID().toString();

        LogMessage requestLog = LogMessage.builder()
                .service("user-service")
                .messageType("REQUEST")
                .message("POST /users -> 201 Created [" + runId + "]")
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();

        LogMessage responseLog = LogMessage.builder()
                .service("transaction-service")
                .messageType("RESPONSE")
                .message("PUT /transactions/transfer/initiation -> 200 OK [" + runId + "]")
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();

        testProducer.send("bank-logs", requestLog);
        testProducer.send("bank-logs", responseLog);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            List<LoggingDump> rows = repository.findAll();
            List<LoggingDump> matches = rows.stream()
                    .filter(row -> row.getMessage() != null && row.getMessage().contains(runId))
                    .toList();

            assertThat(matches).hasSize(2);
            assertThat(matches)
                    .extracting(LoggingDump::getMessageType)
                    .containsExactlyInAnyOrder("REQUEST", "RESPONSE");
        });
    }
}
