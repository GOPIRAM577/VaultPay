package com.payflow.backend.filter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        long currentMinute = Instant.now().getEpochSecond() / 60;
        
        String redisKey = null;
        int limit = 0;

        if ("/payments/initiate".equals(uri) && "POST".equalsIgnoreCase(request.getMethod())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String) {
                String userId = (String) auth.getPrincipal();
                redisKey = "ratelimit:" + userId + ":payment:" + currentMinute;
                limit = 30;
            }
        } else if (uri.startsWith("/auth/")) {
            String ip = getClientIp(request);
            redisKey = "ratelimit:" + ip + ":auth:" + currentMinute;
            limit = 10;
        }

        if (redisKey != null) {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, 60, TimeUnit.SECONDS);
            }

            if (count != null && count > limit) {
                response.setStatus(429); // Too Many Requests
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"RATE_LIMIT_EXCEEDED\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
