package com.mikelcrm.licenseservice.domain.enums;

public enum EstadoCobroMensual {
    PENDIENTE,      // Generado, esperando pago SPEI
    PAGADO,         // Pago recibido y créditos otorgados
    VENCIDO,        // No pagado tras período de gracia
    CONDONADO       // Exonerado manualmente por platform_admin
}
