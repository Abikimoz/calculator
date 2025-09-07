package com.example.test.controller;

import com.example.test.service.CalculationFlowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@WebFluxTest(CalculatorController.class)
public class CalculatorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CalculationFlowService flowService;

    @Test
    public void testCalculate_whenUnordered_returnsUnorderedFlow() {
        int count = 2;
        Flux<String> expectedFlux = Flux.just("1,1,1.23,10", "1,2,4.56,12");
        List<String> expectedList = expectedFlux.collectList().block();

        when(flowService.createUnorderedFlow(anyInt())).thenReturn(expectedFlux);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/calculate")
                        .queryParam("count", count)
                        .queryParam("ordered", "false")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .isEqualTo(expectedList);
    }

    @Test
    public void testCalculate_whenOrdered_returnsOrderedFlow() {
        int count = 2;
        Flux<String> expectedFlux = Flux.just("1,1.2,10,0,3.4,15,0", "2,1.3,11,0,3.5,16,0");
        List<String> expectedList = expectedFlux.collectList().block();

        when(flowService.createOrderedFlow(anyInt())).thenReturn(expectedFlux);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/calculate")
                        .queryParam("count", count)
                        .queryParam("ordered", "true")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .isEqualTo(expectedList);
    }
}