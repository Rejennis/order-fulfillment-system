package com.midlevel.orderfulfillment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous event processing and notifications.
 * 
 * IMPLEMENTATION TIMELINE:
 * - Implemented in Day 4 (Actually Day 7: Domain Events in mentor program)
 * - Enhanced in Day 8: Added dedicated notification executor
 * 
 * @EnableAsync allows @Async methods to run in separate threads.
 * 
 * Benefits:
 * - Event listeners don't block the main transaction
 * - Notifications run asynchronously
 * - Improved response times
 * - Better resource utilization
 * 
 * Configuration:
 * - Uses two thread pools:
 *   1. "taskExecutor" for general async tasks (event listeners, background jobs)
 *   2. Default executor inherited from Spring Boot
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Thread pool executor for async event processing and notifications.
     * 
     * Configuration:
     * - Core pool size: 3 threads (always kept alive)
     * - Max pool size: 10 threads (can scale up under load)
     * - Queue capacity: 50 tasks (buffer for burst traffic)
     * - Thread naming: "async-" prefix for easy identification in logs
     * 
     * @return configured executor for async tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
