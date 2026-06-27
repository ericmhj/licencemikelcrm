package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class OverdueCuotasException extends RuntimeException {

    public OverdueCuotasException(UUID tenantId) {
        super(String.format("Tenant %s has overdue cuotas", tenantId));
    }
}
