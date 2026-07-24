package com.example.BFF_service.service;

import com.example.BFF_service.dto.DashboardResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DashboardService {

    Mono<DashboardResponse> getDashboard(UUID userId);
}