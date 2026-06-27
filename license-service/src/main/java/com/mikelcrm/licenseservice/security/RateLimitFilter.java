package com.mikelcrm.licenseservice.security;

import com.mikelcrm.licenseservice.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Rate limiting filter that uses Redis sliding window counters to enforce
 * per-Tenant request limits. Query API (GET) allows 1000 req/s per Tenant;
 * Command API (POST/PUT/DELETE/PATCH) allows 100 req/s per Tenant.
 *
 * Key format: rate:{tenantId}:{query|command}:{timestamp_second}
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        UUID tenantId = extractTenantId();

        // Skip rate limiting for unauthenticated requests (e.g., health checks)
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        boolean isQuery = HttpMethod.GET.matches(method);
        String apiType = isQuery ? "query" : "command";
        int maxRequests = isQuery
                ? properties.getQueryMaxRequestsPerSecond()
                : properties.getCommandMaxRequestsPerSecond();

        long currentSecond = Instant.now().getEpochSecond();
        String redisKey = String.format("rate:%s:%s:%d", tenantId, apiType, currentSecond);

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount != null && currentCount == 1L) {
            // Key is new, set TTL of 2 seconds for safety margin
            redisTemplate.expire(redisKey, Duration.ofSeconds(2));
        }

        long count = currentCount != null ? currentCount : 0L;

        if (count > maxRequests) {
            log.warn("Rate limit exceeded for tenant {} on {} API: {} > {}",
                    tenantId, apiType, count, maxRequests);
            writeRateLimitExceededResponse(response);
            return;
        }

        long remaining = maxRequests - count;
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(Math.max(remaining, 0)));

        filterChain.doFilter(request, response);
    }

    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TenantAuthenticationToken tenantAuth) {
            return tenantAuth.getTenantId();
        }
        return null;
    }

    private void writeRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\",\"retryAfterSeconds\":1}"
        );
    }
}
