package com.mikelcrm.licenseservice.security;

import java.util.UUID;

/**
 * Holds the authenticated tenant context extracted from the JWT token.
 */
public record TenantPrincipal(UUID tenantId, UUID userId, String rol) {
}
