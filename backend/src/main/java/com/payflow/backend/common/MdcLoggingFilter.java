package com.payflow.backend.common;

import com.payflow.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MdcLoggingFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestId = UUID.randomUUID().toString();
        String path = request.getRequestURI();
        String userId = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtService.validateToken(token)) {
                    userId = jwtService.extractUserId(token);
                }
            } catch (Exception e) {
                // Silently swallow token validation exceptions for logging purposes
            }
        }

        try {
            MDC.put("requestId", requestId);
            MDC.put("path", path);
            if (userId != null) {
                MDC.put("userId", userId);
            } else {
                MDC.put("userId", "anonymous");
            }

            // Expose the Request ID to clients as a response header
            response.setHeader("X-Request-ID", requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
