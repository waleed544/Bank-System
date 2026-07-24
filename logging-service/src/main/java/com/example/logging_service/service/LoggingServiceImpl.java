package com.example.logging_service.service;

import com.example.logging_service.dto.LogMessage;
import com.example.logging_service.entity.LoggingDump;
import com.example.logging_service.repo.LoggingDumpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final LoggingDumpRepository repository;

    @Override
    public void save(LogMessage message) {

        LoggingDump log = LoggingDump.builder()
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .dateTime(message.getTimestamp())
                .build();

        repository.save(log);
    }
}