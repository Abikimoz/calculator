package com.example.test.service;

import reactor.core.publisher.Flux;

public interface CalculationFlowService {

    Flux<String> createOrderedFlow(int count);

    Flux<String> createUnorderedFlow(int count);
}