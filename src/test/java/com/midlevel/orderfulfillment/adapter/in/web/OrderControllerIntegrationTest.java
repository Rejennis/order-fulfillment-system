package com.midlevel.orderfulfillment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midlevel.orderfulfillment.adapter.in.web.dto.AddressDto;
import com.midlevel.orderfulfillment.adapter.in.web.dto.CreateOrderRequest;
import com.midlevel.orderfulfillment.adapter.in.web.dto.MoneyDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController REST endpoints.
 * 
 * Test Strategy:
 * - Use @SpringBootTest to load full application context
 * - Use MockMvc to simulate HTTP requests without starting a server
 * - Use Testcontainers for real PostgreSQL database
 * - Use @Transactional to rollback changes after each test
 * 
 * What we're testing:
 * 1. HTTP layer (request/response, status codes)
 * 2. JSON serialization/deserialization
 * 3. Validation rules
 * 4. Full integration from controller → service → repository → database
 * 
 * These are NOT unit tests - they test the entire stack.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional  // Rollback database changes after each test
@DisplayName("Order REST API Integration Tests")
class OrderControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Should create order and return 201 Created")
    void shouldCreateOrder() throws Exception {
        // Given - prepare request
        CreateOrderRequest request = createValidOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When - POST /api/orders
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Then - verify response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.customerId").value("CUST-123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount.amount").value(149.98))
                .andExpect(jsonPath("$.totalAmount.currency").value("USD"))
                .andExpect(jsonPath("$.shippingAddress.city").value("New York"));
    }
    
    @Test
    @DisplayName("Should return 400 Bad Request for invalid order (empty items)")
    void shouldRejectOrderWithoutItems() throws Exception {
        // Given - order with empty items list
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-123",
                createValidAddress(),
                List.of()  // Empty items - violates business rule
        );
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When - POST /api/orders
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Then - verify validation error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.items").exists());
    }
    
    @Test
    @DisplayName("Should return 400 Bad Request for missing customer ID")
    void shouldRejectOrderWithoutCustomerId() throws Exception {
        // Given - order with null customer ID
        CreateOrderRequest request = new CreateOrderRequest(
                null,  // Missing customer ID
                createValidAddress(),
                createValidItems()
        );
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When - POST /api/orders
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Then - verify validation error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerId").exists());
    }
    
    @Test
    @DisplayName("Should get order by ID")
    void shouldGetOrderById() throws Exception {
        // Given - create an order first
        CreateOrderRequest createRequest = createValidOrderRequest();
        String createJson = objectMapper.writeValueAsString(createRequest);
        
        String orderId = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String extractedOrderId = objectMapper.readTree(orderId).get("orderId").asText();
        
        // When - GET /api/orders/{orderId}
        mockMvc.perform(get("/api/orders/" + extractedOrderId))
                // Then - verify response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(extractedOrderId))
                .andExpect(jsonPath("$.customerId").value("CUST-123"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
    
    @Test
    @DisplayName("Should return 404 Not Found for non-existent order")
    void shouldReturn404ForNonExistentOrder() throws Exception {
        // When - GET /api/orders/{non-existent-id}
        mockMvc.perform(get("/api/orders/NON-EXISTENT-ID"))
                // Then - verify 404
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("Should get orders by customer ID")
    void shouldGetOrdersByCustomerId() throws Exception {
        // Given - create two orders for same customer
        String customerId = "CUST-123";
        CreateOrderRequest request1 = createValidOrderRequest();
        CreateOrderRequest request2 = createValidOrderRequest();
        
        String json1 = objectMapper.writeValueAsString(request1);
        String json2 = objectMapper.writeValueAsString(request2);
        
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json1));
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json2));
        
        // When - GET /api/orders?customerId=CUST-123
        mockMvc.perform(get("/api/orders").param("customerId", customerId))
                // Then - verify both orders returned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerId").value(customerId))
                .andExpect(jsonPath("$[1].customerId").value(customerId));
    }
    
    @Test
    @DisplayName("Should mark order as paid")
    void shouldMarkOrderAsPaid() throws Exception {
        // Given - create an order
        String orderId = createOrderAndGetId();
        
        // When - POST /api/orders/{orderId}/pay
        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                // Then - verify order is paid
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").exists());
    }
    
    @Test
    @DisplayName("Should mark order as shipped (when paid)")
    void shouldMarkOrderAsShipped() throws Exception {
        // Given - create and pay an order
        String orderId = createOrderAndGetId();
        mockMvc.perform(post("/api/orders/" + orderId + "/pay"));
        
        // When - POST /api/orders/{orderId}/ship
        mockMvc.perform(post("/api/orders/" + orderId + "/ship"))
                // Then - verify order is shipped
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shippedAt").exists());
    }
    
    @Test
    @DisplayName("Should return 400 Bad Request when shipping unpaid order")
    void shouldRejectShippingUnpaidOrder() throws Exception {
        // Given - create an order (not paid)
        String orderId = createOrderAndGetId();
        
        // When - POST /api/orders/{orderId}/ship
        mockMvc.perform(post("/api/orders/" + orderId + "/ship"))
                // Then - verify business rule violation
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot ship unpaid order"));
    }
    
    @Test
    @DisplayName("Should cancel order (when not shipped)")
    void shouldCancelOrder() throws Exception {
        // Given - create and pay an order
        String orderId = createOrderAndGetId();
        mockMvc.perform(post("/api/orders/" + orderId + "/pay"));
        
        // When - POST /api/orders/{orderId}/cancel
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
                // Then - verify order is cancelled
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
    
    @Test
    @DisplayName("Should return 400 Bad Request when cancelling shipped order")
    void shouldRejectCancellingShippedOrder() throws Exception {
        // Given - create, pay, and ship an order
        String orderId = createOrderAndGetId();
        mockMvc.perform(post("/api/orders/" + orderId + "/pay"));
        mockMvc.perform(post("/api/orders/" + orderId + "/ship"));
        
        // When - POST /api/orders/{orderId}/cancel
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
                // Then - verify business rule violation
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel shipped order"));
    }
    
    @Test
    @DisplayName("Should get all orders")
    void shouldGetAllOrders() throws Exception {
        // Given - create multiple orders
        createOrderAndGetId();
        createOrderAndGetId();
        
        // When - GET /api/orders/all
        mockMvc.perform(get("/api/orders/all"))
                // Then - verify all orders returned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }
    
    // Helper methods
    
    private String createOrderAndGetId() throws Exception {
        CreateOrderRequest request = createValidOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);
        
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        return objectMapper.readTree(response).get("orderId").asText();
    }
    
    private CreateOrderRequest createValidOrderRequest() {
        return new CreateOrderRequest(
                "CUST-123",
                createValidAddress(),
                createValidItems()
        );
    }
    
    private AddressDto createValidAddress() {
        return new AddressDto(
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "US"
        );
    }
    
    private List<CreateOrderRequest.OrderItemRequest> createValidItems() {
        return List.of(
                new CreateOrderRequest.OrderItemRequest(
                        "PROD-001",
                        "Product 1",
                        new MoneyDto(new BigDecimal("99.99"), "USD"),
                        1
                ),
                new CreateOrderRequest.OrderItemRequest(
                        "PROD-002",
                        "Product 2",
                        new MoneyDto(new BigDecimal("49.99"), "USD"),
                        1
                )
        );
    }
}
