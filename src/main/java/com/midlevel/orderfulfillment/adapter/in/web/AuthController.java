package com.midlevel.orderfulfillment.adapter.in.web;

import com.midlevel.orderfulfillment.adapter.in.web.dto.AuthResponse;
import com.midlevel.orderfulfillment.adapter.in.web.dto.LoginRequest;
import com.midlevel.orderfulfillment.adapter.in.web.dto.RegisterRequest;
import com.midlevel.orderfulfillment.config.security.JwtTokenProvider;
import com.midlevel.orderfulfillment.domain.model.User;
import com.midlevel.orderfulfillment.domain.port.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller - Handles user registration and login.
 * 
 * Provides public endpoints for:
 * - User registration (create new account)
 * - User login (authenticate and get JWT token)
 * 
 * Authentication Flow:
 * 
 * Registration:
 * 1. Client sends username, email, password
 * 2. Validate input (username unique, valid email, strong password)
 * 3. Hash password with BCrypt
 * 4. Save user to database with ROLE_CUSTOMER
 * 5. Generate JWT token
 * 6. Return token to client
 * 
 * Login:
 * 1. Client sends username, password
 * 2. AuthenticationManager validates credentials
 * 3. If valid, generate JWT token
 * 4. Return token to client
 * 5. Client includes token in Authorization header for future requests
 * 
 * Security Considerations:
 * - Passwords hashed with BCrypt (never stored plain text)
 * - JWT tokens have expiration time
 * - Failed login attempts logged
 * - Input validation prevents injection attacks
 * 
 * Day 12 Implementation:
 * - Stateless authentication
 * - JWT token generation
 * - New users get ROLE_CUSTOMER by default
 * - Comprehensive error handling
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * User login endpoint.
     * 
     * POST /api/auth/login
     * 
     * Request body:
     * {
     *   "username": "john",
     *   "password": "password123"
     * }
     * 
     * Response (200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "username": "john",
     *   "roles": "ROLE_CUSTOMER",
     *   "expiresIn": 86400000
     * }
     * 
     * @param loginRequest Login credentials
     * @return Authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.username());
        
        try {
            // Authenticate user with username and password
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );
            
            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Generate JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);
            
            // Get user roles from token
            String roles = jwtTokenProvider.getRolesFromToken(jwt);
            
            log.info("User logged in successfully: {}, roles: {}", loginRequest.username(), roles);
            
            // Return token and user info
            return ResponseEntity.ok(AuthResponse.bearer(
                jwt,
                loginRequest.username(),
                roles,
                jwtTokenProvider.getExpirationMs()
            ));
            
        } catch (Exception e) {
            log.error("Login failed for user: {}, reason: {}", loginRequest.username(), e.getMessage());
            throw new IllegalArgumentException("Invalid username or password");
        }
    }
    
    /**
     * User registration endpoint.
     * 
     * POST /api/auth/register
     * 
     * Request body:
     * {
     *   "username": "john",
     *   "email": "john@example.com",
     *   "password": "password123"
     * }
     * 
     * Response (201 Created):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "username": "john",
     *   "roles": "ROLE_CUSTOMER",
     *   "expiresIn": 86400000
     * }
     * 
     * @param registerRequest Registration details
     * @return Authentication response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for user: {}", registerRequest.username());
        
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.username())) {
            log.warn("Registration failed: username already exists: {}", registerRequest.username());
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Create new user with customer role
        User user = User.createCustomer(
            registerRequest.username(),
            passwordEncoder.encode(registerRequest.password()),
            registerRequest.email()
        );
        
        // Save user to database
        userRepository.save(user);
        
        // Generate JWT token for immediate login
        String jwt = jwtTokenProvider.generateToken(user.getUsername(), user.getRoles());
        
        log.info("User registered successfully: {}, roles: {}", user.getUsername(), user.getRoles());
        
        // Return token and user info
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.bearer(
            jwt,
            user.getUsername(),
            user.getRoles(),
            jwtTokenProvider.getExpirationMs()
        ));
    }
}
