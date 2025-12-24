package com.midlevel.orderfulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Order Fulfillment System application.
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: Marks this as a source of bean definitions
 * - @EnableAutoConfiguration: Enables Spring Boot's auto-configuration
 * - @ComponentScan: Scans for components in this package and sub-packages
 * 
 * This class demonstrates the "Convention over Configuration" principle:
 * - Spring Boot auto-configures based on dependencies in classpath
 * - Minimal configuration needed to get started
 * - Can be customized as needed for production
 */
@SpringBootApplication
public class OrderFulfillmentApplication {
    
    /**
     * Main method to launch the Spring Boot application.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderFulfillmentApplication.class, args);
    }
    
}
