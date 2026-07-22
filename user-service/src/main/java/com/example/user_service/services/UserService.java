package com.example.user_service.services;

import com.example.user_service.dto.*;

import java.util.UUID;

public interface UserService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserProfileResponse getProfile(UUID userId);
}
