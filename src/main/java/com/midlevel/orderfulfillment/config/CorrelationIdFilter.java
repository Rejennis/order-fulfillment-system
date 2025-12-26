package com.midlevel.orderfulfillment.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for Request Tracing (Day 10 - Observability)
 * 
 * Adds a unique correlation ID to every incoming HTTP request, allowing
 * end-to-end tracing across logs, metrics, and distributed services.
 * 
 * Flow:
 * 1. Check if client sent X-Correlation-Id header
 * 2. If yes, use it; if no, generate new UUID
 * 3. Add to MDC (Mapped Diagnostic Context) for logging
 * 4. Add to response header for client tracking
 * 5. Clean up MDC after request completes
 * 
 * Benefits:
 * - Trace single request through all logs
 * - Debug production issues efficiently
 * - Correlate frontend/backend/database operations
 * - Required for distributed tracing
 */
@Component
@Order(1)  // Execute first in filter chain
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Get correlation ID from request header, or generate new one
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Add to MDC so it appears in all logs for this request
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add to response header so client can track the request
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Continue with request processing
            chain.doFilter(request, response);
            
        } finally {
            // Always clean up MDC to prevent memory leaks
            // (Thread pools reuse threads, so MDC must be cleared)
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
