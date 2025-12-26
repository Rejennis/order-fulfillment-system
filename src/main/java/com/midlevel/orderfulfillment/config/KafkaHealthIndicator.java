package com.midlevel.orderfulfillment.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom Health Indicator for Kafka (Day 10 - Observability)
 * 
 * Checks if Kafka cluster is reachable and healthy.
 * Contributes to /actuator/health endpoint.
 * 
 * Health Status:
 * - UP: Kafka cluster is reachable and has active controller
 * - DOWN: Cannot connect to Kafka or cluster unhealthy
 * 
 * This allows monitoring systems to detect Kafka outages and alert accordingly.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            
            // Try to describe the cluster (checks connectivity and cluster health)
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            
            // Wait up to 5 seconds for result
            String clusterId = clusterResult.clusterId().get(5, TimeUnit.SECONDS);
            int nodeCount = clusterResult.nodes().get(5, TimeUnit.SECONDS).size();
            
            // Kafka is healthy if we can get cluster info
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .withDetail("status", "Kafka cluster is reachable")
                    .build();
                    
        } catch (Exception e) {
            // Kafka is down or unreachable
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Cannot connect to Kafka cluster")
                    .build();
        }
    }
}
