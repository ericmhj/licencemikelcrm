package com.mikelcrm.licenseservice.exception;

import java.math.BigDecimal;

public class MontoIncorrectoException extends RuntimeException {

    private final BigDecimal adeudoReal;

    public MontoIncorrectoException(BigDecimal adeudoReal) {
        super(String.format("Monto incorrecto. El adeudo total es: %s", adeudoReal.toPlainString()));
        this.adeudoReal = adeudoReal;
    }

    public BigDecimal getAdeudoReal() {
        return adeudoReal;
    }
}
