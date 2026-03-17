package com.taskmanager.app.security;

import com.taskmanager.app.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterTest {

    @Test
    void doFilterInternal_noAuthHeader_callsChain() throws ServletException, IOException {
        JwtService jwtService = Mockito.mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void doFilterInternal_invalidToken_callsChain() throws ServletException, IOException {
        JwtService jwtService = Mockito.mock(JwtService.class);
        when(jwtService.extractUsernameIfTokenIsValid("badtoken")).thenReturn(null);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer badtoken");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void doFilterInternal_validToken_setsSecurityContext_andCallsChain() throws ServletException, IOException {
        JwtService jwtService = Mockito.mock(JwtService.class);
        when(jwtService.extractUsernameIfTokenIsValid("goodtoken")).thenReturn("dan");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer goodtoken");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }
}

