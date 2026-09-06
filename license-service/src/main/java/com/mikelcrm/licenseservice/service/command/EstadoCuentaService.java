package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.EstadoCuentaTenant;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.TipoMovimientoEdoCuenta;
import com.mikelcrm.licenseservice.domain.repository.EstadoCuentaTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Servicio para registrar movimientos en el estado de cuenta del tenant.
 * Append-only: cada movimiento guarda el saldo resultante.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstadoCuentaService {

    private final EstadoCuentaTenantRepository estadoCuentaRepository;

    /**
     * Registra un ABONO (pago recibido) en el estado de cuenta.
     */
    @Transactional
    public EstadoCuentaTenant registrarAbono(
            Tenant tenant,
            BigDecimal monto,
            String concepto,
            String referencia,
            String claveRastreo,
            String periodoMes,
            UUID cobroMensualId,
            UUID pagoSpeiId) {

        BigDecimal saldoAnterior = obtenerSaldoActual(tenant.getId());
        BigDecimal saldoResultante = saldoAnterior.add(monto);

        EstadoCuentaTenant movimiento = EstadoCuentaTenant.builder()
                .tenant(tenant)
                .tipo(TipoMovimientoEdoCuenta.ABONO)
                .monto(monto)
                .saldoResultante(saldoResultante)
                .concepto(concepto)
                .referencia(referencia)
                .claveRastreo(claveRastreo)
                .periodoMes(periodoMes)
                .cobroMensualId(cobroMensualId)
                .pagoSpeiId(pagoSpeiId)
                .build();

        movimiento = estadoCuentaRepository.save(movimiento);

        log.info("[EdoCuenta] ABONO registrado: tenant={}, monto={}, saldo={}, concepto={}",
                tenant.getId(), monto, saldoResultante, concepto);

        return movimiento;
    }

    /**
     * Registra un CARGO (cobro mensual generado) en el estado de cuenta.
     */
    @Transactional
    public EstadoCuentaTenant registrarCargo(
            Tenant tenant,
            BigDecimal monto,
            String concepto,
            String periodoMes,
            UUID cobroMensualId) {

        BigDecimal saldoAnterior = obtenerSaldoActual(tenant.getId());
        BigDecimal saldoResultante = saldoAnterior.subtract(monto);

        EstadoCuentaTenant movimiento = EstadoCuentaTenant.builder()
                .tenant(tenant)
                .tipo(TipoMovimientoEdoCuenta.CARGO)
                .monto(monto.negate())
                .saldoResultante(saldoResultante)
                .concepto(concepto)
                .periodoMes(periodoMes)
                .cobroMensualId(cobroMensualId)
                .build();

        movimiento = estadoCuentaRepository.save(movimiento);

        log.info("[EdoCuenta] CARGO registrado: tenant={}, monto=-{}, saldo={}, concepto={}",
                tenant.getId(), monto, saldoResultante, concepto);

        return movimiento;
    }

    /**
     * Registra un PAGO DE RENTA mensual del CRM en el estado de cuenta.
     * <p>
     * Modelo de cartera de PREPAGO: la renta se cubre DESCONTANDO del saldo a
     * favor del tenant (previamente abonado). Por eso este movimiento RESTA del
     * saldo, igual que un cargo. El monto se guarda en negativo para reflejar la
     * salida de dinero.
     */
    @Transactional
    public EstadoCuentaTenant registrarPagoRenta(
            Tenant tenant,
            BigDecimal monto,
            String concepto,
            String referencia,
            String claveRastreo,
            String periodoMes,
            UUID pagoSpeiId) {

        // El pago de renta descuenta del saldo a favor (prepago).
        BigDecimal saldoAnterior = obtenerSaldoActual(tenant.getId());
        BigDecimal saldoResultante = saldoAnterior.subtract(monto);

        EstadoCuentaTenant movimiento = EstadoCuentaTenant.builder()
                .tenant(tenant)
                .tipo(TipoMovimientoEdoCuenta.PAGO_RENTA)
                .monto(monto.negate())
                .saldoResultante(saldoResultante)
                .concepto(concepto)
                .referencia(referencia)
                .claveRastreo(claveRastreo)
                .periodoMes(periodoMes)
                .pagoSpeiId(pagoSpeiId)
                .build();

        movimiento = estadoCuentaRepository.save(movimiento);

        log.info("[EdoCuenta] PAGO_RENTA registrado: tenant={}, monto=-{}, saldo={}, periodo={}, concepto={}",
                tenant.getId(), monto, saldoResultante, periodoMes, concepto);

        return movimiento;
    }

    /**
     * Registra un ABONO histórico (reconciliación de movimientos previos de cartera).
     * Permite fijar la fecha original y es idempotente por claveRastreo:
     * si ya existe un movimiento con esa clave, no crea duplicado.
     *
     * @return el movimiento creado, o null si ya existía (idempotencia).
     */
    @Transactional
    public EstadoCuentaTenant registrarAbonoHistorico(
            Tenant tenant,
            BigDecimal monto,
            String concepto,
            String referencia,
            String claveRastreo,
            java.time.LocalDateTime fecha) {

        if (claveRastreo != null && estadoCuentaRepository.existsByClaveRastreo(claveRastreo)) {
            log.info("[EdoCuenta] ABONO histórico ya existente (idempotencia): claveRastreo={}", claveRastreo);
            return null;
        }

        BigDecimal saldoAnterior = obtenerSaldoActual(tenant.getId());
        BigDecimal saldoResultante = saldoAnterior.add(monto);

        EstadoCuentaTenant movimiento = EstadoCuentaTenant.builder()
                .tenant(tenant)
                .tipo(TipoMovimientoEdoCuenta.ABONO)
                .monto(monto)
                .saldoResultante(saldoResultante)
                .concepto(concepto)
                .referencia(referencia)
                .claveRastreo(claveRastreo)
                .registradoEn(fecha != null ? fecha : java.time.LocalDateTime.now())
                .build();

        movimiento = estadoCuentaRepository.save(movimiento);

        log.info("[EdoCuenta] ABONO histórico registrado: tenant={}, monto={}, saldo={}, claveRastreo={}",
                tenant.getId(), monto, saldoResultante, claveRastreo);

        return movimiento;
    }

    /**
     * Obtiene el saldo actual del tenant (último saldo resultante).
     * Si no hay movimientos, retorna 0.
     */
    public BigDecimal obtenerSaldoActual(UUID tenantId) {
        return estadoCuentaRepository.findSaldoActual(tenantId)
                .orElse(BigDecimal.ZERO);
    }
}
