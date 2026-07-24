package com.mikelcrm.licenseservice.exception;

import java.util.List;

public class InvalidRoleException extends RuntimeException {

    private final List<String> invalidRoles;

    public InvalidRoleException(List<String> invalidRoles) {
        super("Roles inválidos: " + invalidRoles);
        this.invalidRoles = invalidRoles;
    }

    public List<String> getInvalidRoles() {
        return invalidRoles;
    }
}
