package com.example.BFF_service.controller;

import com.example.BFF_service.dto.DashboardResponse;
import com.example.BFF_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    @GetMapping("/dashboard/{userId}")
    public Mono<ResponseEntity<DashboardResponse>> getDashboard(
            @PathVariable UUID userId) {

        return dashboardService.getDashboard(userId)
                .map(ResponseEntity::ok);
    }
}