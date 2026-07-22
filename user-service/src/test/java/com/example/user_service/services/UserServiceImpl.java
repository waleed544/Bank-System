package com.example.user_service.services;

import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.dto.RegisterResponse;
import com.example.user_service.repositories.UserRepository;
import org.apache.kafka.security.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest req){
        if(repository.existsByUsername(req.getUsername()))
        {
            throw RegisterResponse.builder().userId(null).message("User Already found").username("Not Found").build();
        }

    }
}
