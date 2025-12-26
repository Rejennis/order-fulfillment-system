package com.midlevel.orderfulfillment.adapter.in.web.dto;

/**
 * Authentication Response DTO - Response for successful login/registration.
 * 
 * Contains JWT token and user information.
 * Client stores token and includes in subsequent requests.
 */
public record AuthResponse(
    String token,
    String type,
    String username,
    String roles,
    long expiresIn // Token expiration time in milliseconds
) {
    /**
     * Create auth response with Bearer token type.
     */
    public static AuthResponse bearer(String token, String username, String roles, long expiresIn) {
        return new AuthResponse(token, "Bearer", username, roles, expiresIn);
    }
}
