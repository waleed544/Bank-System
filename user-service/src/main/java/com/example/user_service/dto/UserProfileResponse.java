package com.example.user_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class UserProfileResponse {

    private UUID userId;

    private String username;

    private String email;

    private String firstName;

    private String lastName;
}
