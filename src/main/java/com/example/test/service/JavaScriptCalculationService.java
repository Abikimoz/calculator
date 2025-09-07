package com.example.test.service;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class JavaScriptCalculationService implements CalculationService {

    @Override
    public Mono<Double> evaluate(String functionString, int argument) {
        return Mono.fromCallable(() -> {
            try (Context context = Context.create("js")) {
                Value function = context.eval("js", "(" + functionString + ")");

                if (!function.canExecute()) {
                    throw new IllegalArgumentException("Предоставленная строка не является исполняемой функцией.");
                }

                Value result = function.execute(argument);

                if (!result.isNumber()) {
                    throw new IllegalStateException("Функция не вернула число.");
                }

                return result.asDouble();
            } catch (Exception e) {
                throw new RuntimeException("Ошибка выполнения JavaScript функции", e);
            }
        });
    }
}