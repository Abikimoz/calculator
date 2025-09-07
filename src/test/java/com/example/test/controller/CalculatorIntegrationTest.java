
package com.example.test.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class CalculatorIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testCalculateUnordered() {
        List<String> result = webTestClient.get()
                .uri("/api/calculate?count=2&ordered=false")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).allMatch(s -> s.matches("^\\d+,\\d+,((\\d+\\.\\d+)|(error: .*)),\\d+$"));
    }

    @Test
    public void testCalculateOrdered() {
        List<String> result = webTestClient.get()
                .uri("/api/calculate?count=2&ordered=true")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> s.matches("^\\d+,((\\d+\\.\\d+,\\d+,\\d+)|(error: .*)),((\\d+\\.\\d+,\\d+,\\d+)|(error: .*))$"));
    }
}
