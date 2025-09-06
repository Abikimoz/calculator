package com.example.test.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class JavaScriptCalculationServiceTest {

    // Сервис создается напрямую, без участия Spring.
    // Это классический модульный тест (unit test).
    private final JavaScriptCalculationService calculationService = new JavaScriptCalculationService();

    @Test
    public void evaluate_shouldReturnCorrectResult_whenFunctionIsValid() {
        // 1. Arrange (Подготовка)
        String function = "function(x) { return x * 2; }";
        int argument = 10;
        float expectedResult = 20.0f;

        // 2. Act (Действие)
        Mono<Float> resultMono = calculationService.evaluate(function, argument);

        // 3. Assert (Проверка)
        // StepVerifier - это специальный инструмент из reactor-test для тестирования
        // реактивных потоков (Mono и Flux).
        StepVerifier.create(resultMono)
                // Ожидаем, что поток выдаст одно значение, равное expectedResult.
                .expectNext(expectedResult)
                // Убеждаемся, что поток успешно завершился.
                .verifyComplete();
    }

    @Test
    public void evaluate_shouldReturnError_whenFunctionIsInvalid() {
        // 1. Arrange (Подготовка)
        // Функция с синтаксической ошибкой (отсутствует закрывающая скобка '}').
        String invalidFunction = "function(x) { return x * 2";
        int argument = 5;

        // 2. Act (Действие)
        Mono<Float> resultMono = calculationService.evaluate(invalidFunction, argument);

        // 3. Assert (Проверка)
        StepVerifier.create(resultMono)
                // Ожидаем, что поток завершится с ошибкой.
                // Мы проверяем, что тип исключения - RuntimeException, так как
                // наш сервис оборачивает любые ошибки от GraalVM в него.
                .expectError(RuntimeException.class)
                // Запускаем проверку.
                .verify();
    }

    @Test
    public void evaluate_shouldReturnError_whenFunctionReturnsNonNumber() {
        // 1. Arrange (Подготовка)
        // Функция возвращает строку, а не число.
        String nonNumberFunction = "function(x) { return 'hello'; }";
        int argument = 5;

        // 2. Act (Действие)
        Mono<Float> resultMono = calculationService.evaluate(nonNumberFunction, argument);

        // 3. Assert (Проверка)
        StepVerifier.create(resultMono)
                // Ожидаем ошибку, так как сервис не сможет преобразовать 'hello' в число.
                .expectError(RuntimeException.class)
                .verify();
    }
}
