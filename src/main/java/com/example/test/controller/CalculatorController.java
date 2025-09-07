package com.example.test.controller;

import com.example.test.service.CalculationFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculationFlowService flowService;

    @GetMapping(value = "/calculate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> calculate(
            @RequestParam("count") int count,
            @RequestParam(value = "ordered", defaultValue = "false") boolean ordered) {

        if (ordered) {
            return flowService.createOrderedFlow(count);
        } else {
            return flowService.createUnorderedFlow(count);
        }
    }
}