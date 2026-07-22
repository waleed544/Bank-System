package com.example.user_service.services;

import com.example.user_service.dto.*;
import com.example.user_service.entities.users;
import com.example.user_service.exceptions.InvalidCredentialsException;
import com.example.user_service.exceptions.UserAlreadyExistsException;
import com.example.user_service.exceptions.UserNotFoundException;
import com.example.user_service.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest req)  {
        if (repository.existsByUsername(req.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (repository.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        users user = new users();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());

        user.setPasswordHash(
                passwordEncoder.encode(req.getPassword())
        );

        user = repository.save(user);
        return RegisterResponse.builder().username(req.getUsername()).message("Registered Successfully").userId(user.getUserId()).build();
    }


    public LoginResponse login(LoginRequest req) {

        users user = repository.findByUsername(req.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return LoginResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }


    public UserProfileResponse getProfile(UUID userId) {

        users user = repository.findById(userId)
                .orElseThrow(() ->
         new UserNotFoundException("User not found"));

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
