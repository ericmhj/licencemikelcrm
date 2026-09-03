package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.CuentaClabeTenant;
import com.mikelcrm.licenseservice.domain.entity.PagoSpeiRecibido;
import com.mikelcrm.licenseservice.domain.repository.CuentaClabeTenantRepository;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registro manual de pagos (sin pasar por Stripe/webhook).
 * Se usa desde el portal de administración para aplicar un pago mensual o un
 * abono prepago a un tenant, reutilizando exactamente la misma lógica que el
 * webhook SPEI (SpeiPaymentService.procesarPagoSpei).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoManualService {

    private final SpeiPaymentService speiPaymentService;
    private final CuentaClabeTenantRepository clabeRepository;

    /**
     * Aplica un pago manual para un tenant. Resuelve la CLABE activa del tipo
     * indicado (FIJO_MENSUAL por defecto) y delega en procesarPagoSpei.
     *
     * @param tenantId       tenant al que se aplica el pago
     * @param monto          monto recibido (MXN)
     * @param claveRastreo   clave de rastreo (idempotencia); si es null se genera una
     * @param referencia     concepto/referencia del pago
     * @return el PagoSpeiRecibido resultante (estado APLICADO/RECHAZADO)
     */
    public PagoSpeiRecibido registrarPagoMensual(
            UUID tenantId,
            BigDecimal monto,
            String claveRastreo,
            String referencia) {

        // Resolver la CLABE activa del tenant (única por tenant). Es la que el
        // webhook usa para identificarlo. Debe existir para aplicar el pago.
        CuentaClabeTenant clabe = clabeRepository
                .findByTenantIdAndActivaTrue(tenantId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El tenant " + tenantId + " no tiene una CLABE activa. " +
                        "Registra una CLABE antes de aplicar el pago."));

        String clave = (claveRastreo != null && !claveRastreo.isBlank())
                ? claveRastreo.trim()
                : "MANUAL-" + UUID.randomUUID();

        log.info("[PagoManual] Aplicando pago mensual manual: tenant={}, monto={}, clave={}",
                tenantId, monto, clave);

        return speiPaymentService.procesarPagoSpei(
                clabe.getClabe(),
                monto,
                clave,
                "PAGO MANUAL (admin)",
                null,
                referencia != null ? referencia : "Pago mensual registrado manualmente",
                null,     // sin stripePaymentIntentId
                null      // sin payloadRaw
        );
    }
}
