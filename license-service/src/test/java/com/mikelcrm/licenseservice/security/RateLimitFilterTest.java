package com.mikelcrm.licenseservice.security;

import com.mikelcrm.licenseservice.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        properties = new RateLimitProperties();
        properties.setQueryMaxRequestsPerSecond(1000);
        properties.setCommandMaxRequestsPerSecond(100);
        properties.setWindowSizeSeconds(1);

        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        filter = new RateLimitFilter(redisTemplate, properties);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void unauthenticatedRequest_skipsRateLimiting() throws Exception {
        // No authentication in SecurityContext
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void authenticatedGetRequest_withinLimit_proceedsNormally() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("GET");
        when(valueOps.increment(anyString())).thenReturn(5L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("995"));
    }

    @Test
    void authenticatedGetRequest_exceedsLimit_returns429() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("GET");
        when(valueOps.increment(anyString())).thenReturn(1001L);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        assertThat(sw.toString()).contains("RATE_LIMIT_EXCEEDED");
        assertThat(sw.toString()).contains("Too many requests");
        assertThat(sw.toString()).contains("retryAfterSeconds");
    }

    @Test
    void authenticatedPostRequest_withinLimit_proceedsNormally() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("POST");
        when(valueOps.increment(anyString())).thenReturn(50L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("50"));
    }

    @Test
    void authenticatedPostRequest_exceedsLimit_returns429() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("POST");
        when(valueOps.increment(anyString())).thenReturn(101L);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    void authenticatedDeleteRequest_usesCommandLimit() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("DELETE");
        when(valueOps.increment(anyString())).thenReturn(101L);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    void authenticatedPutRequest_usesCommandLimit() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("PUT");
        when(valueOps.increment(anyString())).thenReturn(50L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("50"));
    }

    @Test
    void newKey_setsExpire() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("GET");
        when(valueOps.increment(anyString())).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(2)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void existingKey_doesNotSetExpire() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("GET");
        when(valueOps.increment(anyString())).thenReturn(5L);

        filter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void exactlyAtLimit_proceedsNormally() throws Exception {
        setUpAuthentication();
        when(request.getMethod()).thenReturn("POST");
        // Exactly at the limit (100) should still proceed
        when(valueOps.increment(anyString())).thenReturn(100L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("0"));
    }

    private void setUpAuthentication() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantAuthenticationToken auth = new TenantAuthenticationToken(userId, tenantId, "admin");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
