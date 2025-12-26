package com.midlevel.orderfulfillment.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration - Configures Spring Security for JWT-based authentication.
 * 
 * This is the central security configuration that defines:
 * - Which endpoints are public vs protected
 * - How authentication works (JWT)
 * - CORS configuration for cross-origin requests
 * - Password encoding (BCrypt)
 * - Session management (stateless)
 * 
 * Security Model:
 * - Stateless authentication (no sessions)
 * - JWT tokens in Authorization header
 * - Role-based authorization with @PreAuthorize
 * - CSRF disabled (not needed for stateless APIs)
 * 
 * Filter Chain:
 * 1. CorsFilter (handle CORS preflight)
 * 2. JwtAuthenticationFilter (extract and validate JWT)
 * 3. UsernamePasswordAuthenticationFilter (standard Spring Security)
 * 4. Authorization filters (check permissions)
 * 
 * Public Endpoints (no authentication required):
 * - POST /api/auth/login - User login
 * - POST /api/auth/register - User registration
 * - GET /actuator/health - Health check
 * - GET /actuator/prometheus - Metrics (should be secured in production!)
 * 
 * Protected Endpoints (require authentication):
 * - All /api/orders/** endpoints
 * - Authorization determined by roles (@PreAuthorize)
 * 
 * Day 12 Implementation:
 * - Integrates JWT infrastructure
 * - Enables method-level security
 * - Configures stateless sessions
 * - Sets up CORS for frontend integration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize, @Secured, @RolesAllowed
public class SecurityConfiguration {
    
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfiguration(CustomUserDetailsService userDetailsService,
                                 JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    /**
     * Security Filter Chain - Main security configuration.
     * 
     * Configures HTTP security settings including:
     * - CORS (Cross-Origin Resource Sharing)
     * - CSRF (disabled for stateless API)
     * - Authorization rules
     * - Session management (stateless)
     * - JWT filter integration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS, disable CSRF (not needed for stateless JWT)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll() // TODO: Secure in production!
                .requestMatchers("/actuator/metrics/**").permitAll() // TODO: Secure in production!
                
                // Swagger/OpenAPI endpoints (if enabled)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // All other endpoints require authentication
                // Specific role-based authorization is handled by @PreAuthorize on controllers
                .anyRequest().authenticated()
            )
            
            // Stateless session management - no HttpSession created
            // Each request must contain JWT token
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add JWT filter before standard authentication filter
            // This extracts JWT and sets authentication in SecurityContext
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * Authentication Provider - Configures how authentication is performed.
     * 
     * Uses:
     * - CustomUserDetailsService to load users
     * - BCryptPasswordEncoder to verify passwords
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Authentication Manager - Manages authentication process.
     * 
     * Required for login endpoint to authenticate username/password.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Password Encoder - BCrypt for hashing passwords.
     * 
     * BCrypt advantages:
     * - Adaptive (configurable work factor)
     * - Salted automatically (rainbow table resistant)
     * - Industry standard for password storage
     * - Slow by design (brute force resistant)
     * 
     * Never store plain text passwords!
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * CORS Configuration - Allow cross-origin requests from frontend.
     * 
     * In development: Allow all origins
     * In production: Restrict to specific frontend domains
     * 
     * Allows:
     * - All HTTP methods (GET, POST, PUT, DELETE)
     * - Authorization header (for JWT)
     * - All standard headers
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // In production, replace with specific origins: ["https://app.example.com"]
        configuration.setAllowedOrigins(Arrays.asList("*"));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allow all headers (including Authorization for JWT)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
