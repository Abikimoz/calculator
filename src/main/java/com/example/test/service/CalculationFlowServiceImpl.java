package com.example.test.service;

import com.example.test.model.CalculationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Реализация сервиса для создания потоков вычислений.
 */
@Service
@RequiredArgsConstructor
public class CalculationFlowServiceImpl implements CalculationFlowService {

    private final CalculationConfig config;
    private final CalculationService calculationService;

    /**
     * Внутренний класс для хранения результата вычисления и времени его выполнения.
     */
    private record TimedResult(float result, long time) {}

    /**
     * Создает неупорядоченный поток, где результаты отправляются по мере готовности.
     */
    @Override
    public Flux<String> createUnorderedFlow(int count) {
        // Создаем поток "тиков" с заданным интервалом. Нумерация начинается с 0, поэтому добавляем 1.
        Flux<Long> iterations = Flux.interval(Duration.ofMillis(config.getInterval()))
                .take(count)
                .map(i -> i + 1);

        // Для каждого тика запускаем вычисление функции 1.
        Flux<String> flow1 = iterations.flatMap(iter ->
                calculateAndFormatUnordered(config.getFunction1(), 1, iter)
        );

        // И функции 2.
        Flux<String> flow2 = iterations.flatMap(iter ->
                calculateAndFormatUnordered(config.getFunction2(), 2, iter)
        );

        // Объединяем результаты из обоих потоков. Оператор merge передает элементы дальше
        // сразу после их появления в любом из источников.
        return Flux.merge(flow1, flow2);
    }

    /**
     * Создает упорядоченный поток, где результаты для каждой итерации синхронизируются.
     */
    @Override
    public Flux<String> createOrderedFlow(int count) {
        Flux<Long> iterations = Flux.interval(Duration.ofMillis(config.getInterval()))
                .take(count)
                .map(i -> i + 1);

        // Для каждого тика мы запускаем оба вычисления и ждем их завершения с помощью Mono.zip.
        return iterations.flatMap(iter -> {
            Mono<TimedResult> result1Mono = timedCalculation(config.getFunction1(), iter.intValue());
            Mono<TimedResult> result2Mono = timedCalculation(config.getFunction2(), iter.intValue());

            // Mono.zip объединяет результаты двух Mono. Он ждет, пока оба не завершатся,
            // а затем передает оба результата дальше в виде кортежа (tuple).
            return Mono.zip(result1Mono, result2Mono)
                    .map(tuple -> {
                        TimedResult r1 = tuple.getT1();
                        TimedResult r2 = tuple.getT2();
                        // Форматируем строку согласно требованиям для упорядоченного вывода.
                        // <№ итерации>, <рез. функции 1>, <время 1>, <бф. функции 1>, <рез. функции 2>, <время 2>, <бф. функции 2>
                        // Подсчет буферизации пока не реализован, поэтому временно ставим 0.
                        return String.format("%d,%.4f,%d,%d,%.4f,%d,%d",
                                iter, r1.result, r1.time, 0, r2.result, r2.time, 0);
                    });
        });
    }

    /**
     * Выполняет вычисление и замеряет время выполнения.
     */
    private Mono<TimedResult> timedCalculation(String function, int argument) {
        long startTime = System.currentTimeMillis();
        return calculationService.evaluate(function, argument)
                .map(result -> {
                    long endTime = System.currentTimeMillis();
                    return new TimedResult(result, endTime - startTime);
                });
    }

    /**
     * Выполняет вычисление и форматирует результат для неупорядоченного потока.
     */
    private Mono<String> calculateAndFormatUnordered(String function, int functionId, long iteration) {
        long startTime = System.currentTimeMillis();
        return calculationService.evaluate(function, (int) iteration)
                .map(result -> {
                    long endTime = System.currentTimeMillis();
                    // <№ итерации>, <№ функции>, <результат>, <время>
                    return String.format("%d,%d,%.4f,%d", iteration, functionId, result, endTime - startTime);
                })
                .onErrorResume(e -> {
                    // В случае ошибки в скрипте, форматируем строку ошибки согласно заданию.
                    // Пример: 3,1, error: ReferenceError: z is not defined
                    String errorMessage = String.format("%d,%d,error: %s", iteration, functionId, e.getCause().getMessage());
                    return Mono.just(errorMessage);
                });
    }
}
