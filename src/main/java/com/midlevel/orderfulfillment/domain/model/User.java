package com.midlevel.orderfulfillment.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * User - Domain entity for authentication and authorization.
 * 
 * Represents system users with roles for access control.
 * 
 * Domain Responsibilities:
 * - Store user credentials (username, hashed password)
 * - Track user roles for authorization
 * - Maintain audit timestamps
 * 
 * Security Considerations:
 * - Password must NEVER be stored in plain text
 * - Use BCryptPasswordEncoder for hashing
 * - Username is unique identifier
 * - Roles determine access permissions
 * 
 * Roles:
 * - ROLE_CUSTOMER: Can create and view own orders
 * - ROLE_WAREHOUSE_STAFF: Can ship orders
 * - ROLE_ADMIN: Full access to all operations
 * 
 * Day 12 Addition:
 * - Separate from Order domain (different aggregate)
 * - Uses JPA for persistence
 * - Integrates with Spring Security
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password; // BCrypt hashed password
    
    @Column(length = 100)
    private String email;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    /**
     * User roles - stored as comma-separated string for simplicity.
     * In larger systems, use @ManyToMany with Role entity.
     * 
     * Example: "ROLE_CUSTOMER,ROLE_ADMIN"
     */
    @Column(nullable = false)
    private String roles = "ROLE_CUSTOMER";
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
    
    // Constructors
    
    public User() {
    }
    
    public User(String id, String username, String password, String email, boolean enabled, String roles, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.enabled = enabled;
        this.roles = roles;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getRoles() {
        return roles;
    }
    
    public void setRoles(String roles) {
        this.roles = roles;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Factory method to create a new user with customer role.
     * Password should already be encoded with BCrypt.
     * 
     * @param username Username
     * @param encodedPassword BCrypt-encoded password
     * @param email Email address
     * @return New User instance
     */
    public static User createCustomer(String username, String encodedPassword, String email) {
        User user = new User();
        user.username = username;
        user.password = encodedPassword;
        user.email = email;
        user.roles = "ROLE_CUSTOMER";
        user.enabled = true;
        return user;
    }
    
    /**
     * Add a role to this user.
     * 
     * @param role Role to add (should start with "ROLE_")
     */
    public void addRole(String role) {
        if (!roles.contains(role)) {
            roles = roles + "," + role;
        }
    }
    
    /**
     * Check if user has a specific role.
     * 
     * @param role Role to check
     * @return true if user has role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Get roles as a Set.
     * 
     * @return Set of role strings
     */
    public Set<String> getRolesAsSet() {
        Set<String> roleSet = new HashSet<>();
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles.split(",")) {
                roleSet.add(role.trim());
            }
        }
        return roleSet;
    }
}
