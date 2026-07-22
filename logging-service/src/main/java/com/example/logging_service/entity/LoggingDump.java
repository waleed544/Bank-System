package com.example.logging_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_dump")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoggingDump {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}