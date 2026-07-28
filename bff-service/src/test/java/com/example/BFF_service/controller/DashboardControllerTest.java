package com.example.BFF_service.controller;

import com.example.BFF_service.dto.AccountDto;
import com.example.BFF_service.dto.DashboardResponse;
import com.example.BFF_service.exception.DownstreamServiceException;
import com.example.BFF_service.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getDashboard_returns200_withAggregatedData() {
        UUID userId = UUID.randomUUID();

        DashboardResponse response = DashboardResponse.builder()
                .userId(userId)
                .username("hussein_test")
                .accounts(List.of(AccountDto.builder()
                        .accountNumber("1000000001")
                        .transactions(List.of())
                        .build()))
                .build();

        when(dashboardService.getDashboard(eq(userId))).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/bff/dashboard/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("hussein_test")
                .jsonPath("$.accounts[0].accountNumber").isEqualTo("1000000001");
    }

    @Test
    void getDashboard_returns500_whenDownstreamServiceFails() {
        UUID userId = UUID.randomUUID();

        when(dashboardService.getDashboard(eq(userId)))
                .thenReturn(Mono.error(new DownstreamServiceException(
                        "Failed to retrieve dashboard data due to an issue with downstream services.")));

        webTestClient.get()
                .uri("/bff/dashboard/{userId}", userId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").isEqualTo(
                        "Failed to retrieve dashboard data due to an issue with downstream services.");
    }
}
