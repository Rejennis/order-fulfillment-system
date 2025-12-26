package com.midlevel.orderfulfillment.config.security;

import com.midlevel.orderfulfillment.domain.model.User;
import com.midlevel.orderfulfillment.domain.port.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom User Details Service - Loads user data for Spring Security authentication.
 * 
 * This service is called by Spring Security during authentication to:
 * 1. Load user from database by username
 * 2. Return UserDetails object with credentials and authorities
 * 3. Let Spring Security compare passwords
 * 
 * Integration with Spring Security:
 * - Implements UserDetailsService interface (Spring Security contract)
 * - Called by AuthenticationManager during login
 * - Converts domain User to Spring Security UserDetails
 * 
 * Authentication Flow:
 * 1. User submits username/password
 * 2. AuthenticationManager calls loadUserByUsername()
 * 3. We load User from database
 * 4. Return UserDetails with encoded password
 * 5. Spring Security compares passwords with BCryptPasswordEncoder
 * 6. If match, authentication successful
 * 
 * Day 12 Implementation:
 * - Bridges domain User model with Spring Security
 * - Converts roles string to GrantedAuthority collection
 * - Handles user not found scenario
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Load user by username for authentication.
     * 
     * Called by Spring Security's AuthenticationManager during login.
     * 
     * @param username Username to load
     * @return UserDetails object for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Load user from database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> 
                    new UsernameNotFoundException("User not found with username: " + username));
        
        // Convert domain User to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // Already BCrypt encoded
                .authorities(getAuthorities(user))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }
    
    /**
     * Convert user roles to Spring Security GrantedAuthority collection.
     * 
     * Spring Security uses GrantedAuthority for authorization decisions.
     * Each role becomes a SimpleGrantedAuthority.
     * 
     * @param user Domain user entity
     * @return Collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return user.getRolesAsSet().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
