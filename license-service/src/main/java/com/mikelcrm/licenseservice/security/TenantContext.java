package com.mikelcrm.licenseservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility class to retrieve the current TenantPrincipal from the SecurityContextHolder.
 */
public final class TenantContext {

    private TenantContext() {
        // utility class
    }

    /**
     * Returns the current TenantPrincipal from the security context.
     *
     * @return Optional containing the TenantPrincipal if authenticated, empty otherwise
     */
    public static Optional<TenantPrincipal> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TenantAuthenticationToken tenantAuth) {
            return Optional.of(tenantAuth.toTenantPrincipal());
        }
        if (authentication != null && authentication.getPrincipal() instanceof TenantPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Returns the current TenantPrincipal or throws if not authenticated.
     *
     * @return the current TenantPrincipal
     * @throws IllegalStateException if no authenticated principal is available
     */
    public static TenantPrincipal require() {
        return current().orElseThrow(() ->
                new IllegalStateException("No authenticated TenantPrincipal in security context"));
    }
}
