package com.example.test.controller;

import com.example.test.service.CalculationFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST-контроллер для сервиса "Калькулятор".
 * Предоставляет конечную точку /api/calculate для запуска вычислений.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculationFlowService flowService;

    /**
     * Обрабатывает запрос на вычисление.
     *
     * @param count Количество итераций, которое необходимо выполнить.
     * @param ordered Флаг, определяющий, должен ли вывод быть упорядоченным (синхронным).
     * @return Поток (Flux) строк, представляющих собой CSV-данные. Тип контента - text/event-stream.
     */
    @GetMapping(value = "/calculate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> calculate(
            @RequestParam("count") int count,
            @RequestParam(value = "ordered", defaultValue = "false") boolean ordered) {

        // В зависимости от параметра 'ordered' вызываем соответствующий метод сервиса.
        if (ordered) {
            return flowService.createOrderedFlow(count);
        } else {
            return flowService.createUnorderedFlow(count);
        }
    }
}
