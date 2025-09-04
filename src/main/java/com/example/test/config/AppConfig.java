package com.example.test.config;

import com.example.test.model.CalculationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Конфигурационный класс приложения.
 * Отвечает за загрузку внешних конфигураций и предоставление их в виде бинов Spring.
 */
@Configuration
public class AppConfig {

    /**
     * Создает бин CalculationConfig из внешнего JSON-файла.
     * Конфигурация загружается из файла 'config.json', расположенного в classpath.
     *
     * @param objectMapper Jackson ObjectMapper для десериализации JSON.
     * @return Экземпляр CalculationConfig, готовый для внедрения в другие компоненты.
     * @throws IOException если файл конфигурации не может быть прочитан.
     */
    @Bean
    public CalculationConfig calculationConfig(ObjectMapper objectMapper) throws IOException {
        // Используем ClassPathResource для загрузки файла из classpath.
        // Это стандартный способ Spring для доступа к ресурсам.
        ClassPathResource resource = new ClassPathResource("config.json");

        // Читаем входной поток из ресурса и используем ObjectMapper для преобразования
        // JSON в наш Java-объект CalculationConfig.
        return objectMapper.readValue(resource.getInputStream(), CalculationConfig.class);
    }
}
