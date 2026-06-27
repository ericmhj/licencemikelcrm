package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class DuplicateContractException extends RuntimeException {

    private final UUID tenantId;
    private final String modulo;

    public DuplicateContractException(UUID tenantId, String modulo) {
        super(String.format("Tenant %s already has an ACTIVE contract for module %s", tenantId, modulo));
        this.tenantId = tenantId;
        this.modulo = modulo;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getModulo() {
        return modulo;
    }
}
