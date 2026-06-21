package com.lowkeyarhan.TypeAhead.common.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// Aspect-Oriented Programming (AOP) component to measure execution latency of the suggest endpoint.
@Aspect
@Component
public class LatencyAspect {

    private final MetricsService metricsService;

    public LatencyAspect(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // Intercepts the SuggestionController suggest method to measure latency.
    @Around("execution(* com.lowkeyarhan.TypeAhead.modules.suggestion.SuggestionController.suggest(..))")
    public Object measureLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.nanoTime() - start;
            metricsService.recordSuggestLatency(duration);
        }
    }
}
