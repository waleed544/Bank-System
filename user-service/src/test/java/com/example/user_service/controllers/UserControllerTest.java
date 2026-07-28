package com.example.user_service.controllers;

import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RegisterResponse;
import com.example.user_service.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ---------- POST /users/register ----------

    @Test
    void register_returns201_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.register(any())).thenReturn(
                RegisterResponse.builder()
                        .userId(userId)
                        .username("hussein_test")
                        .message("Registered Successfully")
                        .build());

        String body = """
                {
                  "username": "hussein_test",
                  "password": "SomePassword123",
                  "email": "hussein@example.com",
                  "firstName": "Hussein",
                  "lastName": "Hossam"
                }
                """;

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("hussein_test"))
                .andExpect(jsonPath("$.message").value("Registered Successfully"));
    }

    @Test
    void register_returns400_whenUsernameAndPasswordMissing() throws Exception {
        String body = """
                {
                  "email": "hussein@example.com",
                  "firstName": "Hussein",
                  "lastName": "Hossam"
                }
                """;

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.username").value("username is required"))
                .andExpect(jsonPath("$.message.password").value("password is required"));
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        String body = """
                {
                  "username": "hussein_test",
                  "password": "short",
                  "email": "hussein@example.com",
                  "firstName": "Hussein",
                  "lastName": "Hossam"
                }
                """;

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.password").value("password must be at least 8 characters"));
    }

    @Test
    void register_returns400_whenEmailIsInvalid() throws Exception {
        String body = """
                {
                  "username": "hussein_test",
                  "password": "SomePassword123",
                  "email": "not-an-email",
                  "firstName": "Hussein",
                  "lastName": "Hossam"
                }
                """;

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.email").value("email must be a valid email address"));
    }

    // ---------- POST /users/login ----------

    @Test
    void login_returns200_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.login(any())).thenReturn(
                LoginResponse.builder()
                        .userId(userId)
                        .username("hussein_test")
                        .build());

        String body = """
                {
                  "username": "hussein_test",
                  "password": "SomePassword123"
                }
                """;

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("hussein_test"));
    }

    @Test
    void login_returns400_whenFieldsMissing() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.username").value("username is required"))
                .andExpect(jsonPath("$.message.password").value("password is required"));
    }
}
