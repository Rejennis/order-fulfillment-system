package com.midlevel.orderfulfillment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Metrics Configuration (Day 10 - Observability)
 * 
 * Defines business-level metrics for monitoring order fulfillment operations.
 * These metrics are exposed via /actuator/prometheus for scraping by Prometheus.
 * 
 * Metric Types:
 * - Counter: Monotonically increasing value (e.g., total orders created)
 * - Timer: Measures duration and count (e.g., order creation time)
 * - Gauge: Current value that can go up/down (e.g., pending orders)
 * 
 * Best Practices:
 * - Use consistent naming: <domain>.<noun>.<verb>
 * - Add meaningful tags for filtering (status, type, etc.)
 * - Don't create too many unique metrics (cardinality explosion)
 * - Prefer timers over separate count+duration metrics
 */
@Configuration
public class MetricsConfiguration {

    /**
     * Counter for total orders created
     */
    @Bean
    public Counter ordersCreatedCounter(MeterRegistry registry) {
        return Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Counter for failed order operations
     */
    @Bean
    public Counter orderFailuresCounter(MeterRegistry registry) {
        return Counter.builder("orders.failures.total")
                .description("Total number of failed order operations")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Counter for order state transitions (by status)
     */
    @Bean
    public Counter orderStatusChangeCounter(MeterRegistry registry) {
        return Counter.builder("orders.status.changes.total")
                .description("Total number of order status changes")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Timer for order creation operations
     */
    @Bean
    public Timer orderCreationTimer(MeterRegistry registry) {
        return Timer.builder("orders.creation.duration")
                .description("Time taken to create orders")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Counter for events published to Kafka
     */
    @Bean
    public Counter eventsPublishedCounter(MeterRegistry registry) {
        return Counter.builder("events.published.total")
                .description("Total number of events published")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Counter for events consumed from Kafka
     */
    @Bean
    public Counter eventsConsumedCounter(MeterRegistry registry) {
        return Counter.builder("events.consumed.total")
                .description("Total number of events consumed")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Counter for event processing failures
     */
    @Bean
    public Counter eventFailuresCounter(MeterRegistry registry) {
        return Counter.builder("events.failures.total")
                .description("Total number of event processing failures")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }

    /**
     * Counter for notifications sent
     */
    @Bean
    public Counter notificationsSentCounter(MeterRegistry registry) {
        return Counter.builder("notifications.sent.total")
                .description("Total number of notifications sent")
                .tag("domain", "order-fulfillment")
                .register(registry);
    }
}
