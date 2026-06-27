package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class RenewalNotAllowedException extends RuntimeException {

    public RenewalNotAllowedException(UUID contractId, String reason) {
        super(String.format("Renewal not allowed for contract %s: %s", contractId, reason));
    }
}
