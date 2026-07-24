package com.example.BFF_service.config;

import com.example.BFF_service.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient userWebClient;


    public Mono<UserProfileResponse> getUser(UUID userId) {
        return userWebClient
                .get()
                .uri("/users/{userId}/profile", userId)
                .retrieve()
                .bodyToMono(UserProfileResponse.class);
    }
}