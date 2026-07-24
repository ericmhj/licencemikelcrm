package com.mikelcrm.licenseservice.exception;

public class DuplicateRoleSetException extends RuntimeException {

    private final String conflictingCodigo;

    public DuplicateRoleSetException(String conflictingCodigo) {
        super("Ya existe una relación con ese conjunto de roles: " + conflictingCodigo);
        this.conflictingCodigo = conflictingCodigo;
    }

    public String getConflictingCodigo() {
        return conflictingCodigo;
    }
}
