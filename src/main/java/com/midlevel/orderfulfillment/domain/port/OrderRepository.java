package com.midlevel.orderfulfillment.domain.port;

import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.model.OrderStatus;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for Order persistence (Hexagonal Architecture).
 * 
 * HEXAGONAL ARCHITECTURE PATTERN:
 * - This is a "Port" - an interface defined in the domain
 * - The implementation (Adapter) is in the infrastructure layer
 * - Domain depends on abstraction, not concrete implementation
 * - Adapter depends on domain (dependency points inward)
 * 
 * Benefits:
 * - Domain is isolated from infrastructure concerns
 * - Easy to test domain with fake implementations
 * - Can swap implementations (e.g., PostgreSQL -> MongoDB)
 * - Clear boundaries between layers
 */
public interface OrderRepository {
    
    /**
     * Saves a new order or updates an existing one.
     * 
     * @param order the order to save
     * @return the saved order with any generated values
     */
    Order save(Order order);
    
    /**
     * Finds an order by its ID.
     * 
     * @param orderId the order ID
     * @return Optional containing the order if found, empty otherwise
     */
    Optional<Order> findById(String orderId);
    
    /**
     * Finds all orders for a customer.
     * 
     * @param customerId the customer ID
     * @return list of orders, may be empty
     */
    List<Order> findByCustomerId(String customerId);
    
    /**
     * Finds all orders with a specific status.
     * 
     * @param status the order status
     * @return list of orders, may be empty
     */
    List<Order> findByStatus(OrderStatus status);
    
    /**
     * Finds all orders.
     * Use with caution - could return large datasets.
     * 
     * @return list of all orders
     */
    List<Order> findAll();
    
    /**
     * Deletes an order by ID.
     * Note: In real systems, consider soft deletes instead.
     * 
     * @param orderId the order ID to delete
     */
    void deleteById(String orderId);
    
    /**
     * Checks if an order exists.
     * 
     * @param orderId the order ID
     * @return true if exists, false otherwise
     */
    boolean existsById(String orderId);
}
