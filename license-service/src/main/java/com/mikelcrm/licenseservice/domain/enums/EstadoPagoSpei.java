package com.mikelcrm.licenseservice.domain.enums;

public enum EstadoPagoSpei {
    RECIBIDO,       // Webhook recibido del PSP
    APLICADO,       // Créditos otorgados al tenant
    RECHAZADO,      // Monto incorrecto o tenant inválido
    CONCILIADO      // Verificado contra estado de cuenta
}
