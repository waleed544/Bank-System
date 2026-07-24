package com.example.logging_service.service;

import com.example.logging_service.dto.LogMessage;

public interface LoggingService {

    void save(LogMessage message);

}