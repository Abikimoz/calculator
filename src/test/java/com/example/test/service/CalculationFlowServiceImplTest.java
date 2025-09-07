
package com.example.test.service;

import com.example.test.model.CalculationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CalculationFlowServiceImplTest {

    @Mock
    private CalculationConfig config;

    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private CalculationFlowServiceImpl flowService;

    @BeforeEach
    void setUp() {
        when(config.getInterval()).thenReturn(1);
        when(config.getFunction1()).thenReturn("x -> x");
        when(config.getFunction2()).thenReturn("x -> x * 2");
    }

    @Test
    void createUnorderedFlow_shouldProduceCorrectOutput() {
        when(calculationService.evaluate(anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    String function = invocation.getArgument(0);
                    int arg = invocation.getArgument(1);
                    if (function.equals("x -> x")) {
                        return Mono.just((double) arg);
                    }
                    return Mono.just((double) arg * 2);
                });

        Flux<String> result = flowService.createUnorderedFlow(2);

        StepVerifier.create(result)
                .expectNextCount(4)
                .thenConsumeWhile(s -> s.matches("^\\d+,\\d+,((\\d+\\.\\d+)|(error: .*)),\\d+$"))
                .verifyComplete();
    }

    @Test
    void createOrderedFlow_shouldProduceCorrectOutput() {
        when(calculationService.evaluate(anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    String function = invocation.getArgument(0);
                    int arg = invocation.getArgument(1);
                    if (function.equals("x -> x")) {
                        return Mono.delay(Duration.ofMillis(10)).thenReturn((double) arg);
                    }
                    return Mono.delay(Duration.ofMillis(20)).thenReturn((double) arg * 2);
                });

        Flux<String> result = flowService.createOrderedFlow(2);

        StepVerifier.create(result)
                .expectNextMatches(s -> s.matches("^1,1\\.0000,\\d+,1,2\\.0000,\\d+,0$"))
                .expectNextMatches(s -> s.matches("^2,2\\.0000,\\d+,0,4\\.0000,\\d+,0$"))
                .verifyComplete();
    }

    @Test
    void createUnorderedFlow_shouldHandleErrors() {
        when(calculationService.evaluate(anyString(), anyInt()))
                .thenReturn(Mono.just(1.0))
                .thenReturn(Mono.error(new RuntimeException("Calculation failed")));

        Flux<String> result = flowService.createUnorderedFlow(1);

        StepVerifier.create(result)
                .expectNextMatches(s -> s.contains("1.0000"))
                .expectNextMatches(s -> s.contains("error: Calculation failed"))
                .verifyComplete();
    }
}
