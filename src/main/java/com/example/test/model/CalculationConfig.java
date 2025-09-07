package com.example.test.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CalculationConfig {

    private String function1;

    private String function2;

    private int interval;
}