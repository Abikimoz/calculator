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

    // SOLID (D): Принцип инверсии зависимостей.
    // Модули верхнего уровня (контроллер) не должны зависеть от модулей нижнего уровня (реализация сервиса).
    // Оба должны зависеть от абстракций. Здесь контроллер зависит от интерфейса CalculationFlowService.
    //
    // ООП: Полиморфизм.
    // Контроллер зависит от абстракции (интерфейса) CalculationFlowService, а не от
    // конкретного класса CalculationFlowServiceImpl. Это позволяет в будущем легко
    // подменить реализацию логики создания потоков, не меняя код контроллера.
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

        // Здесь происходит полиморфный вызов. Мы обращаемся к методам интерфейса,
        // а какая конкретно реализация будет вызвана, определяется на этапе выполнения.
        if (ordered) {
            return flowService.createOrderedFlow(count);
        } else {
            return flowService.createUnorderedFlow(count);
        }
    }
}
