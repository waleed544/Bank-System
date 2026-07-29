package com.example.user_service.integration;

import com.example.user_service.entities.users;
import com.example.user_service.kafka.LogProducer;
import com.example.user_service.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test: real Spring context, real Postgres (via
 * Testcontainers), real HTTP layer through MockMvc, real PasswordEncoder.
 * Kafka's LogProducer is mocked since this test targets the REST + DB flow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class UserIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bank_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private LogProducer logProducer;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void register_persistsRealUserWithHashedPassword() throws Exception {
        String requestBody = """
                {
                  "username": "hussein",
                  "password": "plainPassword123",
                  "email": "hussein@example.com",
                  "firstName": "Hussein",
                  "lastName": "Hossam"
                }
                """;

        String responseJson = mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("hussein"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID userId = UUID.fromString(
                objectMapper.readTree(responseJson).get("userId").asText());

        Optional<users> saved = userRepository.findById(userId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getEmail()).isEqualTo("hussein@example.com");
        // Password must be hashed, never stored in plaintext.
        assertThat(saved.get().getPasswordHash()).isNotEqualTo("plainPassword123");
        assertThat(passwordEncoder.matches("plainPassword123", saved.get().getPasswordHash())).isTrue();
    }

    @Test
    void register_returns400_whenUsernameAlreadyTaken() throws Exception {
        userRepository.save(users.builder()
                .username("hussein")
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("whatever123"))
                .firstName("Existing")
                .lastName("User")
                .build());

        String requestBody = """
                {
                  "username": "hussein",
                  "password": "plainPassword123",
                  "email": "new@example.com",
                  "firstName": "New",
                  "lastName": "User"
                }
                """;

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void login_succeedsWithRealHashedPassword() throws Exception {
        users user = userRepository.save(users.builder()
                .username("hussein")
                .email("hussein@example.com")
                .passwordHash(passwordEncoder.encode("correctPassword123"))
                .firstName("Hussein")
                .lastName("Hossam")
                .build());

        String requestBody = """
                {
                  "username": "hussein",
                  "password": "correctPassword123"
                }
                """;

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId().toString()));
    }

    @Test
    void login_returns4xx_whenPasswordIsWrong() throws Exception {
        userRepository.save(users.builder()
                .username("hussein")
                .email("hussein@example.com")
                .passwordHash(passwordEncoder.encode("correctPassword123"))
                .firstName("Hussein")
                .lastName("Hossam")
                .build());

        String requestBody = """
                {
                  "username": "hussein",
                  "password": "wrongPassword"
                }
                """;

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_returnsRealPersistedUser() throws Exception {
        users user = userRepository.save(users.builder()
                .username("hussein")
                .email("hussein@example.com")
                .passwordHash(passwordEncoder.encode("somePassword123"))
                .firstName("Hussein")
                .lastName("Hossam")
                .build());

        mockMvc.perform(get("/users/{userId}/profile", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("hussein"))
                .andExpect(jsonPath("$.email").value("hussein@example.com"));
    }

    @Test
    void getProfile_returns4xx_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/users/{userId}/profile", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
