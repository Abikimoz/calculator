package com.example.test.service;

import reactor.core.publisher.Mono;

/**
 * Интерфейс для сервисов, которые могут выполнять скриптовые функции.
 * Эта абстракция позволяет взаимозаменяемо использовать различные скриптовые движки
 * (например, JavaScript, Python).
 */
public interface CalculationService {

    /**
     * Асинхронно вычисляет заданную скриптовую функцию с целочисленным аргументом.
     *
     * @param function Строковое представление функции для выполнения.
     * @param argument Целочисленный аргумент, который будет передан в функцию.
     * @return Mono, который вернет результат вычисления в виде float.
     */
    Mono<Float> evaluate(String function, int argument);
}
