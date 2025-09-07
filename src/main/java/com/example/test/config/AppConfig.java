package com.example.test.config;

import com.example.test.model.CalculationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class AppConfig {

    @Bean
    public CalculationConfig calculationConfig(ObjectMapper objectMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource("config.json");

        return objectMapper.readValue(resource.getInputStream(), CalculationConfig.class);
    }
}