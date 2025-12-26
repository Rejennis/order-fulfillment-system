package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.model.OrderStatus;
import com.midlevel.orderfulfillment.domain.port.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * - Observability (metrics, structured logging): Added in Day 10
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
 * 5. Observability: Logging and metrics (Day 10)
 * 
 * What NOT to put here:
 * - Business rules (those go in Order/OrderItem domain models)
 * - HTTP concerns (request/response mapping - that's in controllers)
 * - Persistence details (that's in adapters)
 */
@Service
@Transactional(readOnly = true)  // All methods read-only by default for performance
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final Counter ordersCreatedCounter;
    private final Counter orderFailuresCounter;
    private final Counter orderStatusChangeCounter;
    private final Timer orderCreationTimer;
    
    public OrderService(
            OrderRepository orderRepository, 
            DomainEventPublisher eventPublisher,
            Counter ordersCreatedCounter,
            Counter orderFailuresCounter,
            Counter orderStatusChangeCounter,
            Timer orderCreationTimer) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.ordersCreatedCounter = ordersCreatedCounter;
        this.orderFailuresCounter = orderFailuresCounter;
        this.orderStatusChangeCounter = orderStatusChangeCounter;
        this.orderCreationTimer = orderCreationTimer;
    }
    
    /**
     * Create and save a new order.
     * 
     * Transaction management:
     * @Transactional ensures this operation is atomic - either fully succeeds or fully fails.
     * 
     * Event publishing happens AFTER the transaction commits successfully.
     * 
     * Observability (Day 10):
     * - Logs order creation with structured data
     * - Records metrics (counter + timer)
     * - Includes correlation ID in logs (from MDC)
     */
    @Transactional  // Write operation - overrides read-only
    public Order createOrder(Order order) {
        log.info("Creating order for customer: {}, items: {}", 
                order.getCustomerId(), order.getItems().size());
        
        return orderCreationTimer.record(() -> {
            try {
                // Domain validation already happened in Order.create()
                // Service just orchestrates the save operation
                Order savedOrder = orderRepository.save(order);
                
                // Publish domain events after successful save
                // This happens AFTER transaction commit
                eventPublisher.publishEvents(savedOrder);
                
                // Record successful creation
                ordersCreatedCounter.increment();
                log.info("Order created successfully: orderId={}, customerId={}, totalAmount={}", 
                        savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getTotalAmount());
                
                return savedOrder;
            } catch (Exception e) {
                orderFailuresCounter.increment();
                log.error("Failed to create order for customer: {}", order.getCustomerId(), e);
                throw e;
            }
        });
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
        log.info("Marking order as paid: orderId={}", orderId);
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
            
            // Idempotency check: if already paid, return successfully without error
            if (order.getStatus() == OrderStatus.PAID || 
                order.getStatus() == OrderStatus.SHIPPED) {
                // Already in paid or later state - idempotent success
                log.info("Order already paid (idempotent): orderId={}, currentStatus={}", 
                        orderId, order.getStatus());
                return order;
            }
            
            // Domain method enforces business rules
            order.pay();
            
            // Save the state change
            Order savedOrder = orderRepository.save(order);
            
            // Publish domain events
            eventPublisher.publishEvents(savedOrder);
            
            // Record status change metric
            orderStatusChangeCounter.increment();
            log.info("Order marked as paid successfully: orderId={}, previousStatus=CREATED", orderId);
            
            return savedOrder;
        } catch (Exception e) {
            orderFailuresCounter.increment();
            log.error("Failed to mark order as paid: orderId={}", orderId, e);
            throw e;
        }
    }
    
    /**
     * Mark an order as shipped.
     */
    @Transactional  // Write operation
    public Order markOrderAsShipped(String orderId) {
        log.info("Marking order as shipped: orderId={}", orderId);
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
            
            log.debug("Order found, current status: orderId={}, status={}", orderId, order.getStatus());
            
            // Domain method enforces business rules (must be paid first)
            order.ship();
            
            Order savedOrder = orderRepository.save(order);
            
            // Publish domain events
            eventPublisher.publishEvents(savedOrder);
            
            orderStatusChangeCounter.increment();
            log.info("Order marked as shipped successfully: orderId={}, previousStatus=PAID", orderId);
            
            return savedOrder;
        } catch (Exception e) {
            orderFailuresCounter.increment();
            log.error("Failed to mark order as shipped: orderId={}", orderId, e);
            throw e;
        }
    }
    
    /**
     * Cancel an order.
     */
    @Transactional  // Write operation
    public Order cancelOrder(String orderId) {
        log.info("Cancelling order: orderId={}", orderId);
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
            
            OrderStatus previousStatus = order.getStatus();
            log.debug("Order found, current status: orderId={}, status={}", orderId, previousStatus);
            
            // Domain method enforces business rules (can't cancel if shipped)
            order.cancel();
            
            Order savedOrder = orderRepository.save(order);
            
            // Publish domain events
            eventPublisher.publishEvents(savedOrder);
            
            orderStatusChangeCounter.increment();
            log.info("Order cancelled successfully: orderId={}, previousStatus={}", orderId, previousStatus);
            
            return savedOrder;
        } catch (Exception e) {
            orderFailuresCounter.increment();
            log.error("Failed to cancel order: orderId={}", orderId, e);
            throw e;
        }
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
