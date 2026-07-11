package com.mikelcrm.licenseservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Pre-authentication filter that trusts headers injected by the APISIX gateway.
 * 
 * When a request passes through APISIX, the gateway:
 * 1. Validates the JWT (signature, expiration, issuer)
 * 2. Extracts claims and injects them as internal headers
 * 3. Removes the Authorization header
 * 
 * This filter detects those headers and establishes Spring Security authentication
 * without re-validating the JWT (already done at the gateway level).
 * 
 * Headers consumed:
 * - X-Consumer-Id: UUID of the authenticated user (from JWT 'sub' claim)
 * - X-License-Id: License ID (from JWT 'license_id' claim) → used as tenantId
 * - X-Plan-Type: Plan type (from JWT 'plan_type' claim) → used as rol
 * 
 * Security: This filter should ONLY be enabled when running behind APISIX.
 * Direct access to the backend without the gateway would bypass authentication.
 */
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthFilter.class);

    private static final String HEADER_CONSUMER_ID = "X-Consumer-Id";
    private static final String HEADER_LICENSE_ID = "X-License-Id";
    private static final String HEADER_PLAN_TYPE = "X-Plan-Type";

    private final boolean enabled;

    public GatewayHeaderAuthFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String consumerId = request.getHeader(HEADER_CONSUMER_ID);
        String licenseId = request.getHeader(HEADER_LICENSE_ID);
        String planType = request.getHeader(HEADER_PLAN_TYPE);

        // Only trust gateway headers if X-Consumer-Id is present (injected by APISIX)
        if (consumerId != null && !consumerId.isBlank()) {
            try {
                UUID userId = UUID.fromString(consumerId);

                // Use license_id as tenantId (or derive from it)
                UUID tenantId = deriveTenantId(licenseId);

                // Use plan_type as rol, default to USER if not present
                String rol = (planType != null && !planType.isBlank()) ? planType : "USER";

                TenantAuthenticationToken authentication =
                        new TenantAuthenticationToken(userId, tenantId, rol);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Gateway auth established: userId={}, tenantId={}, rol={}",
                        userId, tenantId, rol);

            } catch (IllegalArgumentException e) {
                log.warn("Invalid gateway header value: X-Consumer-Id={}, error={}", consumerId, e.getMessage());
                // Let the request continue without authentication — JwtAuthFilter or Spring will reject it
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Derives a tenant UUID from the license ID string.
     * If the license ID is already a UUID, parse it directly.
     * Otherwise, generate a deterministic UUID from the string (UUID v5 / name-based).
     */
    private UUID deriveTenantId(String licenseId) {
        if (licenseId == null || licenseId.isBlank()) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(licenseId);
        } catch (IllegalArgumentException e) {
            // License ID is not a UUID (e.g., "LIC-TEST-001") — generate deterministic UUID
            return UUID.nameUUIDFromBytes(licenseId.getBytes());
        }
    }
}
