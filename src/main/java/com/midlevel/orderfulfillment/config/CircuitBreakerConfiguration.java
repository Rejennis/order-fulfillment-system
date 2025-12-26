package com.midlevel.orderfulfillment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker configuration for preventing cascade failures.
 * 
 * <p><strong>Day 11: Error Handling & Resilience</strong></p>
 * 
 * <p>Circuit breaker pattern states:</p>
 * <ul>
 *   <li><strong>CLOSED:</strong> Normal operation, requests pass through</li>
 *   <li><strong>OPEN:</strong> Too many failures, requests fail fast (no calls made)</li>
 *   <li><strong>HALF_OPEN:</strong> Testing if service recovered (limited requests)</li>
 * </ul>
 * 
 * <p>Use cases:</p>
 * <ul>
 *   <li>External service calls (payment gateway, shipping API)</li>
 *   <li>Database connections (when DB is overloaded)</li>
 *   <li>Kafka publishing (when Kafka cluster is down)</li>
 * </ul>
 * 
 * <p>Benefits:</p>
 * <ul>
 *   <li>Prevents cascade failures across services</li>
 *   <li>Fast failure when downstream service is unavailable</li>
 *   <li>Automatic recovery detection</li>
 *   <li>Reduces load on failing services (gives them time to recover)</li>
 * </ul>
 */
@Configuration
public class CircuitBreakerConfiguration {
    
    /**
     * Default circuit breaker configuration.
     * 
     * <p>Configuration parameters:</p>
     * <ul>
     *   <li><strong>failureRateThreshold:</strong> 50% - Opens circuit when 50% of calls fail</li>
     *   <li><strong>slowCallRateThreshold:</strong> 50% - Opens circuit when 50% of calls are slow</li>
     *   <li><strong>slowCallDurationThreshold:</strong> 5s - Calls taking longer than 5s are considered slow</li>
     *   <li><strong>waitDurationInOpenState:</strong> 30s - How long to stay open before trying half-open</li>
     *   <li><strong>permittedNumberOfCallsInHalfOpenState:</strong> 3 - Test calls in half-open state</li>
     *   <li><strong>minimumNumberOfCalls:</strong> 5 - Minimum calls before calculating failure rate</li>
     *   <li><strong>slidingWindowSize:</strong> 10 - Number of calls to track for metrics</li>
     * </ul>
     * 
     * @return default circuit breaker configuration
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                // Open circuit when 50% of calls fail
                .failureRateThreshold(50.0f)
                
                // Also consider slow calls as failures
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                
                // Wait 30 seconds before attempting recovery
                .waitDurationInOpenState(Duration.ofSeconds(30))
                
                // Allow 3 test calls in half-open state
                .permittedNumberOfCallsInHalfOpenState(3)
                
                // Need at least 5 calls before calculating rates
                .minimumNumberOfCalls(5)
                
                // Track last 10 calls for sliding window
                .slidingWindowSize(10)
                
                // Enable automatic transition from open to half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                
                .build();
    }
    
    /**
     * Circuit breaker registry with default configuration.
     * 
     * <p>The registry manages all circuit breakers in the application.
     * Each external dependency can have its own named circuit breaker.</p>
     * 
     * @param defaultConfig the default circuit breaker configuration
     * @return circuit breaker registry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig defaultConfig) {
        return CircuitBreakerRegistry.of(defaultConfig);
    }
    
    /**
     * Integrate circuit breaker metrics with Micrometer.
     * 
     * <p>Exports circuit breaker metrics to actuator endpoints:</p>
     * <ul>
     *   <li>resilience4j.circuitbreaker.state</li>
     *   <li>resilience4j.circuitbreaker.calls</li>
     *   <li>resilience4j.circuitbreaker.failure.rate</li>
     *   <li>resilience4j.circuitbreaker.slow.call.rate</li>
     * </ul>
     * 
     * @param circuitBreakerRegistry the circuit breaker registry
     * @param meterRegistry the Micrometer meter registry
     * @return tagged circuit breaker metrics
     */
    @Bean
    public TaggedCircuitBreakerMetrics taggedCircuitBreakerMetrics(
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
    }
}
