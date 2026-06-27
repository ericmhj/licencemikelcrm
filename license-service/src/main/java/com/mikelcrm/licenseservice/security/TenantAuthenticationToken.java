package com.mikelcrm.licenseservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * Custom authentication token that holds tenant context extracted from the JWT.
 * Contains userId (principal), tenantId, and rol for downstream authorization decisions.
 */
public class TenantAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID userId;
    private final UUID tenantId;
    private final String rol;

    public TenantAuthenticationToken(UUID userId, UUID tenantId, String rol) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.userId = userId;
        this.tenantId = tenantId;
        this.rol = rol;
        setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getRol() {
        return rol;
    }

    /**
     * Returns the TenantPrincipal record for backward compatibility with existing code.
     */
    public TenantPrincipal toTenantPrincipal() {
        return new TenantPrincipal(tenantId, userId, rol);
    }
}
