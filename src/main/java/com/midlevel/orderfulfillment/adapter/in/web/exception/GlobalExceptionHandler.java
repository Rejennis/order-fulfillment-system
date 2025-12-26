package com.midlevel.orderfulfillment.adapter.in.web.exception;

import com.midlevel.orderfulfillment.application.OrderService.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses across all REST endpoints.
 * 
 * <p>Uses Spring's @RestControllerAdvice to centralize exception handling,
 * following RFC 7807 Problem Details for HTTP APIs standard.</p>
 * 
 * <p><strong>Day 11: Error Handling & Resilience</strong></p>
 * 
 * <p>Benefits:</p>
 * <ul>
 *   <li>Consistent error response format across all endpoints</li>
 *   <li>Centralized logging of errors</li>
 *   <li>Separation of concerns - controllers don't handle exceptions</li>
 *   <li>Client-friendly error messages with proper HTTP status codes</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle OrderNotFoundException (404 Not Found).
     * 
     * <p>Thrown when attempting to operate on an order that doesn't exist.</p>
     * 
     * @param ex the exception
     * @param request the web request
     * @return ProblemDetail with 404 status
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFoundException(
            OrderNotFoundException ex, 
            WebRequest request) {
        
        log.warn("Order not found: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Order Not Found");
        problemDetail.setType(URI.create("https://api.orderfulfillment.com/errors/order-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
    
    /**
     * Handle IllegalStateException (400 Bad Request).
     * 
     * <p>Thrown when attempting invalid state transitions (e.g., shipping an unpaid order).</p>
     * 
     * @param ex the exception
     * @param request the web request
     * @return ProblemDetail with 400 status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(
            IllegalStateException ex, 
            WebRequest request) {
        
        log.warn("Invalid state transition: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid State Transition");
        problemDetail.setType(URI.create("https://api.orderfulfillment.com/errors/invalid-state"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
    
    /**
     * Handle IllegalArgumentException (400 Bad Request).
     * 
     * <p>Thrown when business rules are violated (e.g., negative quantities, empty orders).</p>
     * 
     * @param ex the exception
     * @param request the web request
     * @return ProblemDetail with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
            IllegalArgumentException ex, 
            WebRequest request) {
        
        log.warn("Invalid argument: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setType(URI.create("https://api.orderfulfillment.com/errors/invalid-argument"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
    
    /**
     * Handle validation errors from @Valid annotation (400 Bad Request).
     * 
     * <p>Thrown when request body validation fails.</p>
     * 
     * @param ex the exception containing validation errors
     * @param request the web request
     * @return ProblemDetail with 400 status and field-level errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex, 
            WebRequest request) {
        
        log.warn("Validation failed: {} field errors", ex.getBindingResult().getFieldErrorCount());
        
        // Collect field-level validation errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for request"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://api.orderfulfillment.com/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("fieldErrors", fieldErrors);
        
        return problemDetail;
    }
    
    /**
     * Handle all other exceptions (500 Internal Server Error).
     * 
     * <p>Catch-all for unexpected exceptions. Logs full stack trace but returns
     * generic message to client to avoid leaking internal details.</p>
     * 
     * @param ex the exception
     * @param request the web request
     * @return ProblemDetail with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
            Exception ex, 
            WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.orderfulfillment.com/errors/internal"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        // Only include exception class in development/debug mode
        // In production, avoid exposing internal implementation details
        if (log.isDebugEnabled()) {
            problemDetail.setProperty("exceptionType", ex.getClass().getSimpleName());
        }
        
        return problemDetail;
    }
}
