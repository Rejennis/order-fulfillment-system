package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application Service for orchestrating Order operations.
 * 
 * What is an Application Service?
 * - Sits between Controllers (adapters) and Domain (core business logic)
 * - Orchestrates domain operations and coordinates workflows
 * - Manages transactions
 * - Does NOT contain business logic (that belongs in the domain)
 * 
 * Responsibilities:
 * 1. Transaction management (@Transactional)
 * 2. Calling domain methods in the right order
 * 3. Interacting with repositories (ports)
 * 4. Exception translation if needed
 * 
 * What NOT to put here:
 * - Business rules (those go in Order/OrderItem domain models)
 * - HTTP concerns (request/response mapping - that's in controllers)
 * - Persistence details (that's in adapters)
 */
@Service
@Transactional(readOnly = true)  // All methods read-only by default for performance
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    /**
     * Create and save a new order.
     * 
     * Transaction management:
     * @Transactional ensures this operation is atomic - either fully succeeds or fully fails.
     */
    @Transactional  // Write operation - overrides read-only
    public Order createOrder(Order order) {
        // Domain validation already happened in Order.create()
        // Service just orchestrates the save operation
        return orderRepository.save(order);
    }
    
    /**
     * Find an order by its ID.
     */
    public Optional<Order> findById(String orderId) {
        return orderRepository.findById(orderId);
    }
    
    /**
     * Find all orders for a customer.
     */
    public List<Order> findByCustomerId(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    /**
     * Find all orders (use with caution - could be huge!).
     * In production, this should be paginated.
     */
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
    
    /**
     * Mark an order as paid.
     * 
     * Workflow:
     * 1. Load order from database
     * 2. Call domain method to transition state
     * 3. Save updated order
     */
    @Transactional  // Write operation
    public Order markOrderAsPaid(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        // Domain method enforces business rules
        order.pay();
        
        // Save the state change
        return orderRepository.save(order);
    }
    
    /**
     * Mark an order as shipped.
     */
    @Transactional  // Write operation
    public Order markOrderAsShipped(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        // Domain method enforces business rules (must be paid first)
        order.ship();
        
        return orderRepository.save(order);
    }
    
    /**
     * Cancel an order.
     */
    @Transactional  // Write operation
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        // Domain method enforces business rules (can't cancel if shipped)
        order.cancel();
        
        return orderRepository.save(order);
    }
    
    /**
     * Exception thrown when an order is not found.
     * Could be moved to a separate file in a larger application.
     */
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
}
