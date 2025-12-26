package com.midlevel.orderfulfillment.adapter.in.web;

import com.midlevel.orderfulfillment.adapter.in.web.dto.CreateOrderRequest;
import com.midlevel.orderfulfillment.adapter.in.web.dto.OrderResponse;
import com.midlevel.orderfulfillment.adapter.in.web.mapper.OrderDtoMapper;
import com.midlevel.orderfulfillment.application.OrderService;
import com.midlevel.orderfulfillment.domain.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API Controller for Order operations.
 * 
 * IMPLEMENTATION TIMELINE:
 * - Implemented in Day 3 (Should have been Day 5: REST API & HTTP Semantics)
 * 
 * Hexagonal Architecture (Ports & Adapters):
 * This is an "inbound adapter" (driving adapter) that:
 * - Receives HTTP requests from clients
 * - Converts DTOs to domain models
 * - Calls the application service (use case)
 * - Converts domain models back to DTOs
 * - Returns HTTP responses
 * 
 * Responsibilities:
 * - HTTP request/response handling
 * - Input validation (@Valid)
 * - DTO ↔ Domain mapping
 * - HTTP status codes
 * - Exception handling (via @ControllerAdvice, see below)
 * 
 * What NOT to put here:
 * - Business logic (that's in Order domain)
 * - Transaction management (that's in OrderService)
 * - Persistence (that's in repository adapters)
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management API")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    private final OrderDtoMapper mapper;
    
    public OrderController(OrderService orderService, OrderDtoMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }
    
    /**
     * Create a new order.
     * 
     * POST /api/orders
     * Request body: CreateOrderRequest (JSON)
     * Response: 201 Created with OrderResponse
     */
    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided items and shipping address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        log.info("Received create order request: customerId={}, itemCount={}", 
                request.customerId(), request.items() != null ? request.items().size() : 0);
        
        // Convert DTO to domain model
        Order order = mapper.toDomain(request);
        
        // Execute business operation
        Order savedOrder = orderService.createOrder(order);
        
        // Convert domain model to response DTO
        OrderResponse response = mapper.toResponse(savedOrder);
        
        log.info("Order created successfully: orderId={}, status={}", 
                savedOrder.getOrderId(), savedOrder.getStatus());
        
        // Return 201 Created with the created order
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get an order by ID.
     * 
     * GET /api/orders/{orderId}
     * Response: 200 OK with OrderResponse, or 404 Not Found
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID") @PathVariable String orderId) {
        
        return orderService.findById(orderId)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all orders for a customer.
     * 
     * GET /api/orders?customerId={customerId}
     * Response: 200 OK with list of OrderResponse
     */
    @GetMapping
    @Operation(summary = "Get orders by customer", description = "Retrieves all orders for a specific customer")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @Parameter(description = "Customer ID") @RequestParam String customerId) {
        
        List<OrderResponse> orders = orderService.findByCustomerId(customerId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Get all orders (use with caution).
     * 
     * GET /api/orders/all
     * Response: 200 OK with list of all orders
     * 
     * Note: In production, this should be paginated to avoid loading too much data.
     */
    @GetMapping("/all")
    @Operation(summary = "Get all orders", description = "Retrieves all orders (should be paginated in production)")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Mark an order as paid.
     * 
     * POST /api/orders/{orderId}/pay
     * Response: 200 OK with updated OrderResponse
     */
    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Mark order as paid", description = "Transitions the order to PAID status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order marked as paid"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    public ResponseEntity<OrderResponse> payOrder(
            @Parameter(description = "Order ID") @PathVariable String orderId) {
        
        log.info("Received pay order request: orderId={}", orderId);
        Order paidOrder = orderService.markOrderAsPaid(orderId);
        log.info("Order payment processed: orderId={}, status={}", orderId, paidOrder.getStatus());
        
        return ResponseEntity.ok(mapper.toResponse(paidOrder));
    }
    
    /**
     * Mark an order as shipped.
     * 
     * POST /api/orders/{orderId}/ship
     * Response: 200 OK with updated OrderResponse
     */
    @PostMapping("/{orderId}/ship")
    @Operation(summary = "Mark order as shipped", description = "Transitions the order to SHIPPED status (must be paid first)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order marked as shipped"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid state transition - order must be paid first")
    })
    public ResponseEntity<OrderResponse> shipOrder(
            @Parameter(description = "Order ID") @PathVariable String orderId) {
        
        log.info("Received ship order request: orderId={}", orderId);
        Order shippedOrder = orderService.markOrderAsShipped(orderId);
        log.info("Order shipping processed: orderId={}, status={}", orderId, shippedOrder.getStatus());
        
        return ResponseEntity.ok(mapper.toResponse(shippedOrder));
    }
    
    /**
     * Cancel an order.
     * 
     * POST /api/orders/{orderId}/cancel
     * Response: 200 OK with updated OrderResponse
     */
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Transitions the order to CANCELLED status (cannot cancel shipped orders)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid state transition - cannot cancel shipped order")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable String orderId) {
        
        log.info("Received cancel order request: orderId={}", orderId);
        Order cancelledOrder = orderService.cancelOrder(orderId);
        log.info("Order cancellation processed: orderId={}, status={}", orderId, cancelledOrder.getStatus());
        
        return ResponseEntity.ok(mapper.toResponse(cancelledOrder));
    }
}
