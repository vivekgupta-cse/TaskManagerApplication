package com.taskmanager.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RequestIdFilterTest {

    @Test
    void doFilter_setsHeaderAndMdc_andRemovesAfterChain() throws IOException, ServletException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Ensure no request id provided
        req.removeHeader(RequestIdFilter.REQUEST_ID_HEADER);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);

        // Response must have X-Request-Id header set
        String header = res.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(header).isNotBlank();

        // MDC should not retain the value after the filter completes
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID)).isNull();
    }

    @Test
    void doFilter_preservesProvidedRequestId() throws IOException, ServletException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        req.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "my-id-123");

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("my-id-123");
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID)).isNull();
    }
}

