package com.example.test.service;

import reactor.core.publisher.Mono;

public interface CalculationService {

    Mono<Double> evaluate(String function, int argument);
}