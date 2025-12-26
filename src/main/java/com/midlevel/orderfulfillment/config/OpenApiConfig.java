package com.midlevel.orderfulfillment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 *
 * <p>Configures Swagger UI for interactive API documentation.
 * Access at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON spec: http://localhost:8080/v3/api-docs
 *
 * <p>Features:
 * - Interactive API testing
 * - Request/response schemas
 * - JWT authentication support
 * - Example payloads
 * - Client SDK generation
 *
 * @see <a href="https://springdoc.org/">Springdoc OpenAPI</a>
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI orderFulfillmentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Fulfillment & Notification System API")
                        .description("""
                                A production-ready backend system demonstrating mid-level Java engineering
                                through practical implementation of Domain-Driven Design, Event-Driven Architecture,
                                and modern DevOps practices.
                                
                                ## Features
                                - Order lifecycle management (CREATED → PAID → SHIPPED → DELIVERED)
                                - Event-driven notifications via Kafka
                                - JWT authentication and role-based authorization
                                - Comprehensive observability and monitoring
                                - Production-grade error handling and resilience
                                
                                ## Authentication
                                Most endpoints require JWT authentication. Follow these steps:
                                1. Register a user via POST /api/auth/register
                                2. Login via POST /api/auth/login to get a JWT token
                                3. Click 'Authorize' button (top right) and enter: Bearer <your-token>
                                4. All subsequent requests will include the token automatically
                                
                                ## Business Rules
                                - Orders must have at least one item
                                - Payment is idempotent (can be called multiple times safely)
                                - Cannot ship unpaid orders
                                - Cannot cancel shipped orders
                                - Only ADMIN users can mark orders as shipped/delivered
                                
                                ## Project Info
                                Built as part of the 14-day "Be Prolific - Gulp Life" Mid-Level Java Developer Mentor Program.
                                Source code: https://github.com/Rejennis/order-fulfillment-system
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rejennis")
                                .url("https://github.com/Rejennis")
                                .email("your.email@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.yourdomain.com")
                                .description("Production server (future)")
                ))
                // Add security requirement for all endpoints by default
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                // Define security scheme for JWT
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login endpoint. " +
                                                "Format: Bearer <token>")));
    }
}

