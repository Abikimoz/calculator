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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationFlowServiceImpl implements CalculationFlowService {

    private final CalculationConfig config;
    private final CalculationService calculationService;

    private final ConcurrentMap<Long, TimedResult> pendingResultsF1 = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TimedResult> pendingResultsF2 = new ConcurrentHashMap<>();

    private record TimedResult(Double result, Long duration, Long endTime, Throwable error) {
        static TimedResult success(double result, long duration, long endTime) {
            return new TimedResult(result, duration, endTime, null);
        }

        static TimedResult failure(long duration, long endTime, Throwable error) {
            return new TimedResult(null, duration, endTime, error);
        }

        boolean isSuccess() {
            return error == null;
        }
    }

    @Override
    public Flux<String> createUnorderedFlow(int count) {
        Flux<Long> iterations = Flux.interval(Duration.ofMillis(config.getInterval()))
                .take(count)
                .map(i -> i + 1);

        Flux<String> flow1 = iterations.flatMap(iter ->
                calculateAndFormatUnordered(config.getFunction1(), 1, iter)
        );

        Flux<String> flow2 = iterations.flatMap(iter ->
                calculateAndFormatUnordered(config.getFunction2(), 2, iter)
        );

        return Flux.merge(flow1, flow2);
    }

    @Override
    public Flux<String> createOrderedFlow(int count) {
        pendingResultsF1.clear();
        pendingResultsF2.clear();

        Flux<Long> iterations = Flux.interval(Duration.ofMillis(config.getInterval()))
                .take(count)
                .map(i -> i + 1);

        return iterations
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(iter -> {
                    Mono<TimedResult> result1Mono = timedCalculation(config.getFunction1(), 1, iter.intValue())
                            .doOnSuccess(r -> {
                                if (r != null && r.isSuccess()) {
                                    pendingResultsF1.put(iter, r);
                                }
                            });
                    Mono<TimedResult> result2Mono = timedCalculation(config.getFunction2(), 2, iter.intValue())
                            .doOnSuccess(r -> {
                                if (r != null && r.isSuccess()) {
                                    pendingResultsF2.put(iter, r);
                                }
                            });

                    return Mono.zip(result1Mono, result2Mono)
                            .map(tuple -> {
                                TimedResult r1 = tuple.getT1();
                                TimedResult r2 = tuple.getT2();

                                if (!r1.isSuccess()) {
                                    Throwable error = r1.error().getCause() != null ? r1.error().getCause() : r1.error();
                                    return String.format(Locale.US, "%d,error: %s in function 1", iter, error.getMessage());
                                }
                                if (!r2.isSuccess()) {
                                    Throwable error = r2.error().getCause() != null ? r2.error().getCause() : r2.error();
                                    return String.format(Locale.US, "%d,error: %s in function 2", iter, error.getMessage());
                                }

                                int bufferCount1 = pendingResultsF1.size() - (pendingResultsF1.containsKey(iter) ? 1 : 0);
                                int bufferCount2 = pendingResultsF2.size() - (pendingResultsF2.containsKey(iter) ? 1 : 0);

                                return String.format(Locale.US, "%d,%.4f,%d,%d,%.4f,%d,%d",
                                        iter, r1.result(), r1.duration(),
                                        Math.max(0, bufferCount1),
                                        r2.result(), r2.duration(),
                                        Math.max(0, bufferCount2));
                            })
                            .doOnNext(s -> {
                                pendingResultsF1.remove(iter);
                                pendingResultsF2.remove(iter);
                            });
                })
                .sequential();
    }

    private Mono<TimedResult> timedCalculation(String function, int functionId, int argument) {
        long startTime = System.currentTimeMillis();
        return calculationService.evaluate(function, argument)
                .map(result -> {
                    long endTime = System.currentTimeMillis();
                    return TimedResult.success(result, endTime - startTime, endTime);
                })
                .onErrorResume(e -> {
                    long endTime = System.currentTimeMillis();
                    log.error("Error in function #{} on iteration {}: {}", functionId, argument,
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    return Mono.just(TimedResult.failure(endTime - startTime, endTime, e));
                });
    }

    private Mono<String> calculateAndFormatUnordered(String function, int functionId, long iteration) {
        long startTime = System.currentTimeMillis();
        return calculationService.evaluate(function, (int) iteration)
                .map(result -> {
                    long endTime = System.currentTimeMillis();
                    return String.format(Locale.US, "%d,%d,%.4f,%d", iteration, functionId, result, endTime - startTime);
                })
                .onErrorResume(e -> {
                    log.error("Error in function #{} on iteration {}: {}", functionId, iteration,
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    return Mono.just(String.format(Locale.US, "%d,%d,error: %s",
                            iteration, functionId, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                });
    }
}