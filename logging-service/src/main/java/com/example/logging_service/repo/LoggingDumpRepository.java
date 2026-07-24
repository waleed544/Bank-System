package com.example.logging_service.repo;

import com.example.logging_service.entity.LoggingDump;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoggingDumpRepository extends JpaRepository<LoggingDump, UUID> {
}