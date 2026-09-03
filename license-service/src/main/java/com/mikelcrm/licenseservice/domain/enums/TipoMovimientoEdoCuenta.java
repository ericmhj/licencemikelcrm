package com.mikelcrm.licenseservice.domain.enums;

public enum TipoMovimientoEdoCuenta {
    ABONO,          // Entrada de dinero como crédito a favor (abono prepago)
    CARGO,          // Consumo (deuda): reportes, almacenamiento, etc.
    PAGO_RENTA      // Pago de renta mensual del CRM (habilita el servicio; no es saldo a favor)
}
