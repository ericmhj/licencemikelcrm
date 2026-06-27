package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class CuotaNotFoundException extends RuntimeException {

    private final UUID cuotaId;

    public CuotaNotFoundException(UUID cuotaId) {
        super("Cuota not found: " + cuotaId);
        this.cuotaId = cuotaId;
    }

    public UUID getCuotaId() {
        return cuotaId;
    }
}
