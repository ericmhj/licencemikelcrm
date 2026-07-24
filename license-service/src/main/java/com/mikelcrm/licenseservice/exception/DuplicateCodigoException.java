package com.mikelcrm.licenseservice.exception;

public class DuplicateCodigoException extends RuntimeException {

    private final String codigo;

    public DuplicateCodigoException(String codigo) {
        super("Ya existe una relación con el código: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
