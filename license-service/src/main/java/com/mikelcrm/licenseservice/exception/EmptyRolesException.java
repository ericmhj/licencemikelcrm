package com.mikelcrm.licenseservice.exception;

public class EmptyRolesException extends RuntimeException {

    public EmptyRolesException() {
        super("La lista de roles no puede estar vacía");
    }
}
