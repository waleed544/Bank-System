package com.example.user_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogMessage {

    private String service;

    private String messageType;

    private String message;

    private LocalDateTime timestamp;
}