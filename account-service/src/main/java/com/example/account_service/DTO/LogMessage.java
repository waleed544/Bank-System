package com.example.account_service.DTO;

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