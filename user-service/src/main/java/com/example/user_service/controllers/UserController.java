package com.example.user_service.controllers;


import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.dto.RegisterResponse;
import com.example.user_service.dto.UserProfileResponse;
import com.example.user_service.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request) {

        RegisterResponse response = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        LoginResponse response = userService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable UUID userId) {

        UserProfileResponse response = userService.getProfile(userId);

        return ResponseEntity.ok(response);
    }
}