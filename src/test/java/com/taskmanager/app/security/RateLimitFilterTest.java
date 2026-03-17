package com.taskmanager.app.security;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RateLimitFilterTest {

    @Test
    void doFilterInternal_nonAuthPath_allowsThrough() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/tasks/list");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void doFilterInternal_authPath_whenAllowed_callsChain() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/login");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // multiple calls should be allowed by default bucket
        filter.doFilterInternal(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void doFilterInternal_authPath_whenRateLimited_returns429() throws ServletException, IOException, NoSuchFieldException, IllegalAccessException {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/login");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Create a mock Bucket that denies consumption
        Bucket mockBucket = mock(Bucket.class);
        when(mockBucket.tryConsume(1)).thenReturn(false);

        // Inject the mock bucket into the filter's private cache
        Field bucketsField = RateLimitFilter.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Cache<String, Bucket> buckets = (Cache<String, Bucket>) bucketsField.get(filter);
        buckets.asMap().put("127.0.0.1", mockBucket);

        filter.doFilterInternal(req, res, chain);

        // Chain should NOT be called and response should be 429 with error message
        verify(chain, never()).doFilter(req, res);
        assertEquals(429, res.getStatus());
        assertTrue(res.getContentAsString().contains("Too many requests"));
    }

}
