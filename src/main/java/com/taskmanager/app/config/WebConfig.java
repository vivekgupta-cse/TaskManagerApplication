package com.taskmanager.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Web MVC configuration.
 *
 * Spring Boot 6+ removed automatic trailing-slash matching.
 * This filter transparently redirects any request ending with '/'
 * (e.g. /api/tasks/) to its canonical form (/api/tasks),
 * so callers don't need to worry about the trailing slash.
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> trailingSlashRedirectFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String uri = request.getRequestURI();

                // Redirect "/api/tasks/" → "/api/tasks"  (preserve query string)
                if (uri.length() > 1 && uri.endsWith("/")) {
                    String cleanUri = uri.substring(0, uri.length() - 1);
                    String queryString = request.getQueryString();
                    String redirectUrl = queryString != null
                            ? cleanUri + "?" + queryString
                            : cleanUri;
                    response.sendRedirect(redirectUrl);
                    return;
                }

                filterChain.doFilter(request, response);
            }
        });

        registration.addUrlPatterns("/api/*");
        registration.setOrder(1); // run before security filter chain
        registration.setName("trailingSlashRedirectFilter");
        return registration;
    }
}

