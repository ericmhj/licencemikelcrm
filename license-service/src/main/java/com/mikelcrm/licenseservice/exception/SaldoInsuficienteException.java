package com.mikelcrm.licenseservice.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    private final BigDecimal saldoActual;
    private final BigDecimal costoRequerido;

    public SaldoInsuficienteException(BigDecimal saldoActual, BigDecimal costoRequerido) {
        super(String.format("Saldo insuficiente. Saldo actual: %s, costo requerido: %s",
                saldoActual.toPlainString(), costoRequerido.toPlainString()));
        this.saldoActual = saldoActual;
        this.costoRequerido = costoRequerido;
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public BigDecimal getCostoRequerido() {
        return costoRequerido;
    }
}
