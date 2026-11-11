package com.crt.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 限流：/api/judge 每 IP 每窗口最多 N 次，超出返回 429
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;

    public RateLimitFilter(@Value("${crt.limit.requests-per-window:10}") int limit,
                           @Value("${crt.limit.window-seconds:60}") int windowSeconds) {
        this.limiter = new RateLimiter(limit, windowSeconds * 1000L);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean limited = "POST".equalsIgnoreCase(request.getMethod())
                && "/api/judge".equals(request.getRequestURI());
        if (!limited) {
            chain.doFilter(request, response);
            return;
        }

        String client = request.getRemoteAddr();
        if (!limiter.tryAcquire(client)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(limiter.retryAfterSeconds(client)));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"请求太频繁，请稍后再试\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
