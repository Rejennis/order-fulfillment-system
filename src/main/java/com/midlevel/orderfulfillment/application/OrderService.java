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
 * IMPLEMENTATION TIMELINE:
 * - Basic CRUD operations: Implemented in Day 3 (Should have been Day 5: REST API)
 * - Event publishing integration: Added in Day 4 (Actually Day 7: Domain Events)
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
    private final DomainEventPublisher eventPublisher;
    
    public OrderService(OrderRepository orderRepository, DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Create and save a new order.
     * 
     * Transaction management:
     * @Transactional ensures this operation is atomic - either fully succeeds or fully fails.
     * 
     * Event publishing happens AFTER the transaction commits successfully.
     */
    @Transactional  // Write operation - overrides read-only
    public Order createOrder(Order order) {
        // Domain validation already happened in Order.create()
        // Service just orchestrates the save operation
        Order savedOrder = orderRepository.save(order);
        
        // Publish domain events after successful save
        // This happens AFTER transaction commit
        eventPublisher.publishEvents(savedOrder);
        
        return savedOrder;
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
     * Idempotency: Calling this method multiple times with the same orderId
     * will not cause errors. If the order is already paid, we return it successfully.
     * 
     * Why idempotent?
     * - Network retries might call this twice
     * - Payment gateway webhooks might duplicate
     * - Prevents "already paid" errors from breaking flows
     * 
     * Workflow:
     * 1. Load order from database
     * 2. Check if already paid (idempotency check)
     * 3. If already paid: return order without error
     * 4. If not paid: call domain method to transition state
     * 5. Save updated order
     * 6. Publish events after transaction commits
     */
    @Transactional  // Write operation
    public Order markOrderAsPaid(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        // Idempotency check: if already paid, return successfully without error
        if (order.getStatus() == OrderStatus.PAID || 
            order.getStatus() == OrderStatus.SHIPPED) {
            // Already in paid or later state - idempotent success
            return order;
        }
        
        // Domain method enforces business rules
        order.pay();
        
        // Save the state change
        Order savedOrder = orderRepository.save(order);
        
        // Publish domain events
        eventPublisher.publishEvents(savedOrder);
        
        return savedOrder;
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
        
        Order savedOrder = orderRepository.save(order);
        
        // Publish domain events
        eventPublisher.publishEvents(savedOrder);
        
        return savedOrder;
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
        
        Order savedOrder = orderRepository.save(order);
        
        // Publish domain events
        eventPublisher.publishEvents(savedOrder);
        
        return savedOrder;
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
