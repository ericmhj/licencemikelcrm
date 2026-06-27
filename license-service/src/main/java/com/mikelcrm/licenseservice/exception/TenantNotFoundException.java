package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {

    private final UUID tenantId;

    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found: " + tenantId);
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
