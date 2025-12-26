package com.midlevel.orderfulfillment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Retry configuration for handling transient failures.
 * 
 * <p><strong>Day 11: Error Handling & Resilience</strong></p>
 * 
 * <p>Spring Retry provides declarative retry support via @Retryable annotation.
 * Use cases:</p>
 * <ul>
 *   <li>Database connection failures (temporary network issues)</li>
 *   <li>External API timeouts (brief service unavailability)</li>
 *   <li>Optimistic locking conflicts (concurrent updates)</li>
 * </ul>
 * 
 * <p>Example usage in service layer:</p>
 * <pre>
 * {@code
 * @Retryable(
 *     value = {DataAccessException.class},
 *     maxAttempts = 3,
 *     backoff = @Backoff(delay = 1000, multiplier = 2.0)
 * )
 * public Order saveOrder(Order order) {
 *     return orderRepository.save(order);
 * }
 * }
 * </pre>
 * 
 * <p><strong>Best Practices:</strong></p>
 * <ul>
 *   <li>Only retry idempotent operations (safe to repeat)</li>
 *   <li>Use exponential backoff to avoid overwhelming the system</li>
 *   <li>Set reasonable max attempts (typically 3-5)</li>
 *   <li>Define specific exceptions to retry (not all exceptions)</li>
 *   <li>Add @Recover method for fallback behavior after all retries fail</li>
 * </ul>
 */
@Configuration
@EnableRetry
public class RetryConfiguration {
    // @EnableRetry activates Spring Retry support
    // No additional configuration needed for basic retry functionality
    // Individual methods can be annotated with @Retryable as needed
}
