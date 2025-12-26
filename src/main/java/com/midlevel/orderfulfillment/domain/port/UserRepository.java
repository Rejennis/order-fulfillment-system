package com.midlevel.orderfulfillment.domain.port;

import com.midlevel.orderfulfillment.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository Port - Contract for user persistence operations.
 * 
 * Follows the same pattern as OrderRepository (Port interface in domain).
 * Implementation is in adapter layer (JPA adapter).
 * 
 * Hexagonal Architecture:
 * - Domain defines the contract (port)
 * - Infrastructure provides implementation (adapter)
 * - Keeps domain independent of persistence technology
 * 
 * Day 12 Addition:
 * - Supports authentication (findByUsername)
 * - Supports registration (save)
 * - Enables user management
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find user by username.
     * Used for authentication - loading user details for login.
     * 
     * @param username Username to search for
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if username already exists.
     * Used for registration validation.
     * 
     * @param username Username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Find user by email.
     * Optional: could be used for password reset functionality.
     * 
     * @param email Email to search for
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);
}
