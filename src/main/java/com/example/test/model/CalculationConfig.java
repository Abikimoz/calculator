package com.example.test.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Этот класс представляет конфигурацию для сервиса калькулятора.
 * Он используется для десериализации конфигурационного JSON-файла,
 * который определяет поведение калькулятора.
 */
@Data
@NoArgsConstructor
public class CalculationConfig {

    /**
     * Первая вычисляемая функция в виде строки.
     * Это может быть код на JavaScript или Python.
     * Пример: "function(x) { return x * x; }"
     */
    private String function1;

    /**
     * Вторая вычисляемая функция в виде строки.
     * Это может быть код на JavaScript или Python.
     * Пример: "function(x) { return x + 10; }"
     */
    private String function2;

    /**
     * Интервал в миллисекундах между итерациями вычислений.
     * Определяет, как часто сервис будет выполнять вычисления.
     */
    private int interval;
}
