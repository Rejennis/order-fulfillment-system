package com.midlevel.orderfulfillment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for the Order Fulfillment System.
 * 
 * This configuration:
 * 1. Enables Kafka support with @EnableKafka
 * 2. Creates topic definitions (topics are auto-created if they don't exist)
 * 3. Configures topic properties (partitions, replication, retention)
 * 
 * Topics follow naming convention: domain.event-type
 * Example: order.created, order.paid, order.shipped
 */
@Configuration
@EnableKafka
public class KafkaConfig {
    
    // Topic names as constants for easy reference and refactoring
    public static final String TOPIC_ORDER_CREATED = "order.created";
    public static final String TOPIC_ORDER_PAID = "order.paid";
    public static final String TOPIC_ORDER_SHIPPED = "order.shipped";
    public static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    public static final String TOPIC_ORDER_EVENTS_DLQ = "order.events.dlq";
    
    /**
     * Create 'order.created' topic.
     * 
     * Configuration:
     * - 3 partitions for parallelism (can handle 3 consumers simultaneously)
     * - Replication factor 1 (dev environment - production should use 3)
     * - Compact cleanup for keeping latest state
     */
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .compact()  // Keep only latest message per key (order ID)
                .build();
    }
    
    /**
     * Create 'order.paid' topic.
     */
    @Bean
    public NewTopic orderPaidTopic() {
        return TopicBuilder.name(TOPIC_ORDER_PAID)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    /**
     * Create 'order.shipped' topic.
     */
    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_SHIPPED)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    /**
     * Create 'order.cancelled' topic.
     */
    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CANCELLED)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    /**
     * Dead Letter Queue (DLQ) topic.
     * 
     * Events that fail processing after retries go here for manual inspection.
     * Uses delete cleanup policy to avoid infinite growth.
     */
    @Bean
    public NewTopic deadLetterQueueTopic() {
        return TopicBuilder.name(TOPIC_ORDER_EVENTS_DLQ)
                .partitions(1)  // Single partition for DLQ (order not critical)
                .replicas(1)
                .config("retention.ms", "604800000")  // 7 days retention
                .build();
    }
}
