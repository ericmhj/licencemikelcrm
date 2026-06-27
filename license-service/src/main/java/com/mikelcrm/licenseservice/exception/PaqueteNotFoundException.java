package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class PaqueteNotFoundException extends RuntimeException {

    public PaqueteNotFoundException(UUID tenantId) {
        super(String.format("No active credit package found for tenant: %s", tenantId));
    }
}
