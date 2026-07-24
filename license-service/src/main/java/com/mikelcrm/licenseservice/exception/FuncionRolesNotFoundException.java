package com.mikelcrm.licenseservice.exception;

public class FuncionRolesNotFoundException extends RuntimeException {

    private final String codigo;

    public FuncionRolesNotFoundException(String codigo) {
        super("Funcion-roles not found: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
