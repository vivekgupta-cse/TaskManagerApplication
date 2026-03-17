package com.taskmanager.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebConfigTest {

    @Test
    void trailingSlashRedirectFilter_redirectsTrailingSlash() throws Exception {
        WebConfig cfg = new WebConfig();
        OncePerRequestFilter filter = cfg.trailingSlashRedirectFilter().getFilter();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/tasks/");
        req.setQueryString("q=1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        // should redirect to /api/tasks?q=1
        assertThat(res.getRedirectedUrl()).isEqualTo("/api/tasks?q=1");
    }

    @Test
    void trailingSlashRedirectFilter_allowsNormalPath() throws Exception {
        WebConfig cfg = new WebConfig();
        OncePerRequestFilter filter = cfg.trailingSlashRedirectFilter().getFilter();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/tasks");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        // No redirect; chain should be invoked
        verify(chain).doFilter(req, res);
        assertThat(res.getRedirectedUrl()).isNull();
    }
}

