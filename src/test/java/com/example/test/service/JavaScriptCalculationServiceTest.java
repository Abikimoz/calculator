package com.example.test.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class JavaScriptCalculationServiceTest {

    private final JavaScriptCalculationService calculationService = new JavaScriptCalculationService();

    @Test
    public void evaluate_shouldReturnCorrectResult_whenFunctionIsValid() {
        String function = "function(x) { return x * 2; }";
        int argument = 10;
        double expectedResult = 20.0;

        Mono<Double> resultMono = calculationService.evaluate(function, argument);

        StepVerifier.create(resultMono)
                .expectNext(expectedResult)
                .verifyComplete();
    }

    @Test
    public void evaluate_shouldReturnError_whenFunctionIsInvalid() {
        String invalidFunction = "function(x) { return x * 2";
        int argument = 5;

        Mono<Double> resultMono = calculationService.evaluate(invalidFunction, argument);

        StepVerifier.create(resultMono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    public void evaluate_shouldReturnError_whenFunctionReturnsNonNumber() {
        String nonNumberFunction = "function(x) { return 'hello'; }";
        int argument = 5;

        Mono<Double> resultMono = calculationService.evaluate(nonNumberFunction, argument);

        StepVerifier.create(resultMono)
                .expectError(RuntimeException.class)
                .verify();
    }
}