package com.taskmanager.app.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // One bucket per IP; allow 10 requests per minute on auth endpoints
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = buckets.get(ip, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                            .build());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP={}", ip);
                response.setStatus(429);
                response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
                return;
            } else {
                log.debug("Rate limit allowance granted for IP={}", ip);
            }
        }
        chain.doFilter(request, response);
    }
}
