package com.example.test.service;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Реализация CalculationService для JavaScript.
 * Этот сервис использует GraalVM Polyglot API для выполнения функций на JavaScript.
 */
@Service
public class JavaScriptCalculationService implements CalculationService {

    /**
     * Вычисляет функцию на JavaScript.
     *
     * Метод создает новый контекст GraalVM для каждого вызова, чтобы обеспечить потокобезопасность
     * и избежать утечки состояния между вызовами функций.
     *
     * @param functionString Строковое представление функции на JavaScript.
     * @param argument Целочисленный аргумент для функции.
     * @return Mono, который вернет результат. В случае ошибки выполнения вернет ошибку.
     */
    @Override
    public Mono<Float> evaluate(String functionString, int argument) {
        // Оборачиваем вызов в Mono.fromCallable для выполнения в реактивном стиле.
        return Mono.fromCallable(() -> {
            // Используем try-with-resources, чтобы гарантировать автоматическое закрытие контекста.
            // Это критически важно для освобождения ресурсов и предотвращения утечек памяти.
            try (Context context = Context.create("js")) {
                // Строка с функцией выполняется в контексте JS.
                // Оборачиваем строку в скобки, чтобы она была обработана как выражение.
                Value function = context.eval("js", "(" + functionString + ")");

                // Проверяем, можно ли выполнить полученное значение как функцию.
                if (!function.canExecute()) {
                    throw new IllegalArgumentException("Предоставленная строка не является исполняемой функцией.");
                }

                // Выполняем функцию с заданным аргументом.
                Value result = function.execute(argument);

                // Проверяем, можно ли преобразовать результат в число.
                if (!result.isNumber()) {
                    throw new IllegalStateException("Функция не вернула число.");
                }

                // Преобразуем результат в float и возвращаем его.
                return result.asFloat();
            } catch (Exception e) {
                // Любое исключение во время выполнения скрипта перехватывается,
                // оборачивается в RuntimeException и передается в конвейер ошибок Mono.
                throw new RuntimeException("Ошибка выполнения JavaScript функции", e);
            }
        });
    }
}
