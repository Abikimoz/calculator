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

/**
 * Интеграционные тесты для CalculatorController.
 *
 * @WebFluxTest(CalculatorController.class) загружает только необходимую часть
 * контекста Spring, связанную с WebFlux и указанным контроллером. Это быстрее,
 * чем загружать весь контекст приложения.
 */
@WebFluxTest(CalculatorController.class)
public class CalculatorControllerTest {

    // WebTestClient - это реактивный клиент для тестирования web-эндпоинтов без
    // запуска реального сервера.
    @Autowired
    private WebTestClient webTestClient;

    // @MockBean создает мок-объект для CalculationFlowService и помещает его
    // в контекст приложения. Контроллер будет использовать этот мок вместо
    // реальной реализации.
    @MockBean
    private CalculationFlowService flowService;

    @Test
    public void testCalculate_whenUnordered_returnsUnorderedFlow() {
        // 1. Arrange (Подготовка)
        int count = 2;
        // Создаем тестовый поток данных, который, как мы ожидаем, вернет наш мок-сервис.
        Flux<String> expectedFlux = Flux.just("1,1,1.23,10", "1,2,4.56,12");
        List<String> expectedList = expectedFlux.collectList().block();

        // Настраиваем поведение мока: когда будет вызван метод createUnorderedFlow с любым
        // целочисленным аргументом, он должен вернуть наш тестовый Flux.
        when(flowService.createUnorderedFlow(anyInt())).thenReturn(expectedFlux);

        // 2. Act & 3. Assert (Действие и Проверка)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/calculate")
                        .queryParam("count", count)
                        .queryParam("ordered", "false")
                        .build())
                .exchange() // Выполняем запрос
                .expectStatus().isOk() // Ожидаем HTTP статус 200 OK
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM) // Проверяем Content-Type
                .expectBodyList(String.class) // Ожидаем, что тело ответа можно будет собрать в список строк
                .isEqualTo(expectedList); // Сравниваем с ожидаемым списком
    }

    @Test
    public void testCalculate_whenOrdered_returnsOrderedFlow() {
        // 1. Arrange (Подготовка)
        int count = 2;
        Flux<String> expectedFlux = Flux.just("1,1.2,10,0,3.4,15,0", "2,1.3,11,0,3.5,16,0");
        List<String> expectedList = expectedFlux.collectList().block();

        // Настраиваем мок для упорядоченного потока
        when(flowService.createOrderedFlow(anyInt())).thenReturn(expectedFlux);

        // 2. Act & 3. Assert (Действие и Проверка)
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
