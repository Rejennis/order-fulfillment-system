package com.midlevel.orderfulfillment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous event processing.
 * 
 * IMPLEMENTATION TIMELINE:
 * - Implemented in Day 4 (Actually Day 7: Domain Events in mentor program)
 * 
 * @EnableAsync allows @Async methods to run in separate threads.
 * 
 * Benefits:
 * - Event listeners don't block the main transaction
 * - Improved response times
 * - Better resource utilization
 * 
 * Configuration:
 * - Uses Spring's default ThreadPoolTaskExecutor
 * - Can be customized with custom executor bean if needed
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    // Using Spring Boot's default async configuration
    // For production, you might want to configure a custom executor:
    /*
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
    */
}
