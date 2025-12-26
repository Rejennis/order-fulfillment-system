package com.midlevel.orderfulfillment.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT Token Provider - Handles JWT token generation and validation.
 * 
 * This component is responsible for:
 * - Generating JWT tokens after successful authentication
 * - Validating JWT tokens from incoming requests
 * - Extracting user information from tokens
 * 
 * JWT Structure:
 * - Header: Algorithm and token type
 * - Payload: User claims (username, roles, expiration)
 * - Signature: Verifies token hasn't been tampered with
 * 
 * Security Considerations:
 * - Secret key must be at least 256 bits (32 characters) for HS256
 * - Token expiration prevents long-lived compromised tokens
 * - Signature ensures token integrity
 * - Stateless: No server-side session storage needed
 * 
 * Token Format: Bearer <token>
 * Example: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * Day 12 Implementation:
 * - Uses JJWT library (io.jsonwebtoken)
 * - Stores roles in token for authorization
 * - Configurable expiration time
 * - Comprehensive error handling
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${security.jwt.secret:ThisIsAVerySecureSecretKeyForJWTTokenGenerationAndValidationPleaseChangeInProduction}")
    private String jwtSecret;
    
    @Value("${security.jwt.expiration-ms:86400000}") // 24 hours default
    private long jwtExpirationMs;
    
    private SecretKey secretKey;
    
    /**
     * Initialize the secret key after bean creation.
     * Converts the configured secret string into a cryptographic key.
     */
    @PostConstruct
    public void init() {
        // Convert string secret to SecretKey for HMAC-SHA256
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Token Provider initialized with expiration: {}ms", jwtExpirationMs);
    }
    
    /**
     * Generate JWT token from Spring Security Authentication object.
     * 
     * Token contains:
     * - Subject: username
     * - Roles: user authorities/roles
     * - Issued At: token creation time
     * - Expiration: token validity end time
     * 
     * @param authentication Spring Security authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        // Extract roles from authentication
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        String token = Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
        
        log.debug("Generated JWT token for user: {}, roles: {}, expires: {}", 
                  username, roles, expiryDate);
        
        return token;
    }
    
    /**
     * Generate token with custom username (used for registration).
     * 
     * @param username Username to include in token
     * @param roles Comma-separated roles
     * @return JWT token string
     */
    public String generateToken(String username, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        String token = Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
        
        log.debug("Generated JWT token for user: {}, roles: {}, expires: {}", 
                  username, roles, expiryDate);
        
        return token;
    }
    
    /**
     * Extract username from JWT token.
     * 
     * @param token JWT token string
     * @return Username (subject claim)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }
    
    /**
     * Extract roles from JWT token.
     * 
     * @param token JWT token string
     * @return Comma-separated roles string
     */
    public String getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("roles", String.class);
    }
    
    /**
     * Validate JWT token.
     * 
     * Checks:
     * - Signature is valid (token not tampered with)
     * - Token not expired
     * - Token format is correct
     * 
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token format: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get expiration time in milliseconds.
     * Useful for clients to know when to refresh tokens.
     * 
     * @return Expiration time in milliseconds
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
