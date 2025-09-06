package com.example.test.service;

import com.example.test.model.CalculationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Реализация сервиса для создания потоков вычислений.
 *
 * SOLID (S): Принцип единственной ответственности.
 * Класс отвечает только за оркестрацию потоков вычислений, делегируя фактические
 * вычисления другому сервису (CalculationService).
 *
 * SOLID (L): Принцип подстановки Лисков.
 * Этот класс может работать с любой реализацией CalculationService, не меняя своего поведения.
 *
 * SOLID (D): Принцип инверсии зависимостей.
 * Класс зависит от абстракции CalculationService, а не от ее конкретной реализации.
 *
 * ООП: Принципы Инкапсуляции и Полиморфизма.
 * 1. Инкапсуляция: Логика создания упорядоченных и неупорядоченных потоков скрыта
 *    внутри этого класса. Внешний мир взаимодействует с ним только через методы,
 *    определенные в интерфейсе CalculationFlowService.
 * 2. Полиморфизм: Класс зависит от абстракции (CalculationService), а не от конкретной
 *    реализации, что позволяет легко подменять способ вычисления (например, с JS на Python).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationFlowServiceImpl implements CalculationFlowService {

    // ООП: Инкапсуляция.
    // Зависимости скрыты от внешнего мира с помощью модификатора private.
    // Они устанавливаются один раз через конструктор (благодаря @RequiredArgsConstructor)
    // и не могут быть изменены в дальнейшем (final).
    private final CalculationConfig config;

    // ООП: Полиморфизм.
    // Поле объявлено через интерфейс (абстракцию) CalculationService.
    // Во время выполнения Spring внедрит сюда конкретную реализацию (JavaScriptCalculationService).
    // Это позволяет менять реализацию, не затрагивая код этого класса.
    private final CalculationService calculationService;

    // Потокобезопасные множества для хранения номеров завершенных итераций.
    private final Set<Long> completedF1Iterations = new ConcurrentSkipListSet<>();
    private final Set<Long> completedF2Iterations = new ConcurrentSkipListSet<>();

    /**
     * Внутренний класс для хранения детального результата вычисления.
     * Содержит либо результат, либо ошибку, а также метаданные.
     */
    private record TimedResult(Float result, Long duration, Long endTime, Throwable error) {
        // Фабричный метод для успешного результата
        static TimedResult success(float result, long duration, long endTime) {
            return new TimedResult(result, duration, endTime, null);
        }

        // Фабричный метод для результата с ошибкой
        static TimedResult failure(long duration, long endTime, Throwable error) {
            return new TimedResult(null, duration, endTime, error);
        }

        boolean isSuccess() {
            return error == null;
        }
    }

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
        // Очищаем множества перед каждым новым потоком вычислений для чистоты состояния.
        completedF1Iterations.clear();
        completedF2Iterations.clear();

        Flux<Long> iterations = Flux.interval(Duration.ofMillis(config.getInterval()))
                .take(count)
                .map(i -> i + 1);

        return iterations
                .parallel() // Принудительно распараллеливаем поток
                .runOn(Schedulers.parallel()) // Указываем использовать общий пул потоков
                .flatMap(iter -> {
                    // При успешном завершении вычисления добавляем номер итерации в соответствующее множество.
                    Mono<TimedResult> result1Mono = timedCalculation(config.getFunction1(), 1, iter.intValue())
                            .doOnSuccess(r -> {
                                if (r != null && r.isSuccess()) completedF1Iterations.add(iter);
                            });
                    Mono<TimedResult> result2Mono = timedCalculation(config.getFunction2(), 2, iter.intValue())
                            .doOnSuccess(r -> {
                                if (r != null && r.isSuccess()) completedF2Iterations.add(iter);
                            });

                    return Mono.zip(result1Mono, result2Mono)
                            .map(tuple -> {
                                TimedResult r1 = tuple.getT1();
                                TimedResult r2 = tuple.getT2();

                                // Если одна из функций вернула ошибку, выводим короткое сообщение
                                if (!r1.isSuccess()) {
                                    Throwable error = r1.error().getCause() != null ? r1.error().getCause() : r1.error();
                                    return String.format(Locale.US, "%d,error: %s in function 1", iter, error.getMessage());
                                }
                                if (!r2.isSuccess()) {
                                    Throwable error = r2.error().getCause() != null ? r2.error().getCause() : r2.error();
                                    return String.format(Locale.US, "%d,error: %s in function 2", iter, error.getMessage());
                                }

                                // Обе функции успешны, формируем полную строку
                                long bufferCount1 = completedF1Iterations.stream().filter(i -> i > iter).count();
                                long bufferCount2 = completedF2Iterations.stream().filter(i -> i > iter).count();

                                return String.format(Locale.US, "%d,%.4f,%d,%d,%.4f,%d,%d",
                                        iter, r1.result(), r1.duration(), bufferCount1, r2.result(), r2.duration(), bufferCount2);
                            })
                            .doOnNext(s -> {
                                // После того как строка для текущей итерации сформирована,
                                // удаляем ее номер из множеств, чтобы они не росли бесконечно.
                                completedF1Iterations.remove(iter);
                                completedF2Iterations.remove(iter);
                            });
                })
                .sequential(); // Собираем результаты обратно в один последовательный поток
    }

    /**
     * Выполняет вычисление и замеряет время, возвращая детальный результат.
     * Перехватывает ошибки, чтобы не прерывать поток.
     */
    private Mono<TimedResult> timedCalculation(String function, int functionId, int argument) {
        long startTime = System.currentTimeMillis();
        return calculationService.evaluate(function, argument)
                .map(result -> {
                    long endTime = System.currentTimeMillis();
                    return TimedResult.success(result, endTime - startTime, endTime);
                })
                .onErrorResume(e -> {
                    long endTime = System.currentTimeMillis();
                    log.error("Error in function #{} on iteration {}: {}", functionId, argument, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    return Mono.just(TimedResult.failure(endTime - startTime, endTime, e));
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
                    return String.format(Locale.US, "%d,%d,%.4f,%d", iteration, functionId, result, endTime - startTime);
                })
                .onErrorResume(e -> {
                    // В случае ошибки в скрипте, форматируем строку ошибки согласно заданию.
                    // Пример: 3,1, error: ReferenceError: z is not defined
                    log.error("Error in function #{} on iteration {}: {}", functionId, iteration, e.getCause().getMessage());
                    String errorMessage = String.format(Locale.US, "%d,%d,error: %s", iteration, functionId, e.getCause().getMessage());
                    return Mono.just(errorMessage);
                });
    }
}
