package com.midlevel.orderfulfillment.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter - Processes JWT tokens from HTTP requests.
 * 
 * This filter intercepts every HTTP request and:
 * 1. Extracts JWT token from Authorization header
 * 2. Validates the token
 * 3. Loads user details from token
 * 4. Sets authentication in SecurityContext
 * 
 * Filter Chain Position:
 * - Runs once per request (OncePerRequestFilter)
 * - Executes before Spring Security's authentication filters
 * - Populates SecurityContext for downstream filters and controllers
 * 
 * Authorization Header Format:
 * Authorization: Bearer <jwt-token>
 * 
 * Security Flow:
 * Request → Extract JWT → Validate → Create Authentication → Set SecurityContext → Continue Chain
 * 
 * If JWT is invalid or missing:
 * - Request continues but SecurityContext remains empty
 * - Spring Security's authorization rules will deny access
 * - Public endpoints can still be accessed
 * 
 * Day 12 Implementation:
 * - Stateless authentication (no sessions)
 * - Extracts username and roles from JWT
 * - Sets Spring Security authentication
 * - Integrates with @PreAuthorize annotations
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenProvider jwtTokenProvider;
    
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * Main filter logic - processes JWT token from request.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain to continue processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Token is valid - extract user details
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String rolesString = jwtTokenProvider.getRolesFromToken(jwt);
                
                // Convert role strings to GrantedAuthority objects
                List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                
                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                // Add request details (IP, session ID, etc.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in SecurityContext
                // This makes the user authenticated for this request
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Authenticated user: {} with roles: {}", username, rolesString);
            }
        } catch (Exception ex) {
            log.error("Failed to set user authentication in security context", ex);
            // Don't throw exception - let request continue without authentication
            // Spring Security will deny access if authentication is required
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     * 
     * Expected format: "Bearer <token>"
     * 
     * @param request HTTP request
     * @return JWT token string, or null if not found/invalid format
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Check if Authorization header exists and starts with "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Extract token after "Bearer " prefix (7 characters)
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
