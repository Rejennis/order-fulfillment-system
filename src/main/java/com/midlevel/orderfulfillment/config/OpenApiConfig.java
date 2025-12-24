package com.midlevel.orderfulfillment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 * 
 * Access the documentation at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 * 
 * Benefits:
 * - Interactive API testing without Postman
 * - Auto-generated from code annotations
 * - Client SDK generation
 * - API contract documentation
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI orderFulfillmentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Fulfillment System API")
                        .description("RESTful API for managing order fulfillment with hexagonal architecture")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mid-Level Java Developer Training")
                                .email("dev@orderfulfillment.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")));
    }
}
