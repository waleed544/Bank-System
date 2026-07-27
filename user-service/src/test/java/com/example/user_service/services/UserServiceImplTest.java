package com.example.user_service.services;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.dto.RegisterResponse;
import com.example.user_service.entities.users;
import com.example.user_service.exceptions.InvalidCredentialsException;
import com.example.user_service.exceptions.UserAlreadyExistsException;
import com.example.user_service.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("hussein");
        registerRequest.setEmail("hussein@example.com");
        registerRequest.setPassword("plainPassword123");
        registerRequest.setFirstName("Hussein");
        registerRequest.setLastName("Hossam");
    }

    // ---------- register() ----------

    @Test
    void register_savesNewUser_whenUsernameAndEmailAreFree() {
        when(repository.existsByUsername("hussein")).thenReturn(false);
        when(repository.existsByEmail("hussein@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("hashed-password");

        UUID generatedId = UUID.randomUUID();
        when(repository.save(any(users.class))).thenAnswer(invocation -> {
            users toSave = invocation.getArgument(0);
            toSave.setUserId(generatedId);
            return toSave;
        });

        RegisterResponse response = userService.register(registerRequest);

        assertThat(response.getUserId()).isEqualTo(generatedId);
        assertThat(response.getUsername()).isEqualTo("hussein");
        assertThat(response.getMessage()).isEqualTo("Registered Successfully");

        verify(passwordEncoder).encode("plainPassword123");
        verify(repository).save(any(users.class));
    }

    @Test
    void register_throwsUserAlreadyExists_whenUsernameTaken() {
        when(repository.existsByUsername("hussein")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username already exists");

        verify(repository).existsByUsername("hussein");
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void register_throwsUserAlreadyExists_whenEmailTaken() {
        when(repository.existsByUsername("hussein")).thenReturn(false);
        when(repository.existsByEmail("hussein@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already exists");

        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    // ---------- login() ----------

    @Test
    void login_returnsUserId_whenCredentialsAreValid() {
        UUID userId = UUID.randomUUID();
        users existingUser = users.builder()
                .userId(userId)
                .username("hussein")
                .passwordHash("hashed-password")
                .email("hussein@example.com")
                .build();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("hussein");
        loginRequest.setPassword("plainPassword123");

        when(repository.findByUsername("hussein")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("plainPassword123", "hashed-password")).thenReturn(true);

        LoginResponse response = userService.login(loginRequest);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("hussein");
    }

    @Test
    void login_throwsInvalidCredentials_whenUsernameNotFound() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("ghost");
        loginRequest.setPassword("whatever");

        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        users existingUser = users.builder()
                .userId(UUID.randomUUID())
                .username("hussein")
                .passwordHash("hashed-password")
                .email("hussein@example.com")
                .build();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("hussein");
        loginRequest.setPassword("wrongPassword");

        when(repository.findByUsername("hussein")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
