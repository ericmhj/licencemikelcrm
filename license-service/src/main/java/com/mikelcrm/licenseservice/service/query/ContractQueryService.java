package com.mikelcrm.licenseservice.service.query;

import com.mikelcrm.licenseservice.domain.entity.*;
import com.mikelcrm.licenseservice.domain.enums.*;
import com.mikelcrm.licenseservice.domain.repository.*;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.query.dto.ContractResponse;
import com.mikelcrm.licenseservice.service.query.dto.CreditResponse;
import com.mikelcrm.licenseservice.service.query.dto.ReactivationSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractQueryService {

    private final TenantRepository tenantRepository;
    private final ContratoAnualRepository contratoRepository;
    private final CuotaMensualRepository cuotaMensualRepository;
    private final PaqueteCreditosRepository paqueteRepository;
    private final ContadorConsultasRepository contadorRepository;
    private final CuotaAlmacenamientoRepository cuotaAlmacenamientoRepository;

    /**
     * Lists all contracts for a tenant with status, paid installments, and discount available today.
     */
    public ContractResponse getContracts(UUID tenantId) {
        List<ContratoAnual> contratos = contratoRepository.findByTenantId(tenantId);

        List<ContractResponse.ContractDetail> details = contratos.stream()
                .map(this::mapContractDetail)
                .collect(Collectors.toList());

        return ContractResponse.builder()
                .contracts(details)
                .build();
    }

    /**
     * Returns credit balance, threshold, global counter, excess, and active package info.
     */
    public CreditResponse getCredits(UUID tenantId) {
        PaqueteCreditos paquete = paqueteRepository
                .findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE)
                .orElse(null);

        BigDecimal saldo = paquete != null ? paquete.getSaldoDisponible() : BigDecimal.ZERO;
        int creditosTotales = paquete != null ? paquete.getCreditosTotalesAdquiridos() : 0;
        int umbral = creditosTotales * 100;

        int periodoAnio = LocalDate.now().getYear();
        int contadorGlobal = contadorRepository.sumTotalConsultasByTenantIdAndPeriodoAnio(tenantId, periodoAnio);
        int excedente = Math.max(contadorGlobal - umbral, 0);

        CreditResponse.PaqueteActivo paqueteActivo = null;
        if (paquete != null) {
            paqueteActivo = CreditResponse.PaqueteActivo.builder()
                    .paqueteId(paquete.getId())
                    .creditosPaquete(paquete.getCreditosPaquete())
                    .creditosBonus(paquete.getCreditosBonus())
                    .fechaVencimiento(paquete.getFechaVencimiento())
                    .build();
        }

        // Determine alert level based on remaining percentage
        String alertaNivel = null;
        if (paquete != null && creditosTotales > 0) {
            BigDecimal totalAdquirido = BigDecimal.valueOf(creditosTotales);
            double percentage = saldo.doubleValue() / totalAdquirido.doubleValue() * 100;
            if (percentage <= 0) {
                alertaNivel = "CRITICO";
            } else if (percentage <= 10) {
                alertaNivel = "BAJO";
            } else if (percentage <= 20) {
                alertaNivel = "ADVERTENCIA";
            }
        }

        return CreditResponse.builder()
                .saldoDisponible(saldo)
                .creditosTotalesAdquiridos(creditosTotales)
                .umbralConsultasIncluidas(umbral)
                .contadorGlobalConsultas(contadorGlobal)
                .excedente(excedente)
                .paqueteActivo(paqueteActivo)
                .alertaNivel(alertaNivel)
                .build();
    }

    /**
     * Returns a breakdown of total debt for reactivation of a suspended tenant.
     */
    public ReactivationSummaryResponse getReactivationSummary(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        long diasEnMora = 0;
        if (tenant.getFechaSuspension() != null) {
            diasEnMora = ChronoUnit.DAYS.between(
                    tenant.getFechaSuspension().toLocalDate(), LocalDate.now());
        }

        // Overdue cuotas
        List<CuotaMensual> cuotasVencidas = cuotaMensualRepository
                .findByTenantIdAndEstadoIn(tenantId, List.of(EstadoCuota.VENCIDA, EstadoCuota.EN_MORA));

        List<ReactivationSummaryResponse.CuotaVencidaDetail> cuotaDetails = cuotasVencidas.stream()
                .map(c -> ReactivationSummaryResponse.CuotaVencidaDetail.builder()
                        .cuotaId(c.getId())
                        .periodo(c.getFechaLimite().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .monto(c.getMontoOriginal())
                        .fechaLimite(c.getFechaLimite())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalCuotasVencidas = cuotasVencidas.stream()
                .map(CuotaMensual::getMontoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Storage fees
        List<CuotaAlmacenamiento> almacenamientos = cuotaAlmacenamientoRepository
                .findByTenantIdAndEstado(tenantId, EstadoCuotaAlmacenamiento.PENDIENTE);

        List<ReactivationSummaryResponse.CuotaAlmacenamientoDetail> almacenamientoDetails = almacenamientos.stream()
                .map(a -> ReactivationSummaryResponse.CuotaAlmacenamientoDetail.builder()
                        .periodo(a.getPeriodoMes().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .reportes(a.getReportesSnapshot())
                        .monto(a.getMonto())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalAlmacenamiento = almacenamientos.stream()
                .map(CuotaAlmacenamiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalParaReactivar = totalCuotasVencidas.add(totalAlmacenamiento);

        return ReactivationSummaryResponse.builder()
                .tenantId(tenantId)
                .diasEnMora(diasEnMora)
                .cuotasVencidas(cuotaDetails)
                .cuotasAlmacenamiento(almacenamientoDetails)
                .totalCuotasVencidas(totalCuotasVencidas)
                .totalAlmacenamiento(totalAlmacenamiento)
                .totalParaReactivar(totalParaReactivar)
                .build();
    }

    private ContractResponse.ContractDetail mapContractDetail(ContratoAnual contrato) {
        // Calculate next payment date from pending cuotas
        List<CuotaMensual> cuotas = cuotaMensualRepository.findByContratoId(contrato.getId());
        LocalDate proximaFechaCobro = cuotas.stream()
                .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE)
                .map(CuotaMensual::getFechaLimite)
                .min(LocalDate::compareTo)
                .orElse(null);

        // Calculate discount available today
        int descuentoHoy = calcularDescuentoHoy(proximaFechaCobro);
        BigDecimal montoConDescuento = calcularMontoConDescuento(contrato.getCuotaMensual(), descuentoHoy);

        return ContractResponse.ContractDetail.builder()
                .contratoId(contrato.getId())
                .tipo(contrato.getTipo().name())
                .modulo(contrato.getModulo())
                .estado(contrato.getEstado().name())
                .cuotaMensual(contrato.getCuotaMensual())
                .cuotasPagadas(contrato.getCuotasPagadas())
                .cuotasTotales(contrato.getCuotasTotales())
                .proximaFechaCobro(proximaFechaCobro)
                .fechaVencimientoContrato(contrato.getFechaVencimiento())
                .renovacionAuto(contrato.getRenovacionAuto())
                .descuentoDisponibleHoy(descuentoHoy)
                .montoConDescuentoHoy(montoConDescuento)
                .build();
    }

    private int calcularDescuentoHoy(LocalDate proximaFechaCobro) {
        if (proximaFechaCobro == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(proximaFechaCobro)) {
            return 10; // 10% discount for early payment
        } else if (today.isEqual(proximaFechaCobro)) {
            return 3;  // 3% discount on the due date
        }
        return 0; // No discount after due date
    }

    private BigDecimal calcularMontoConDescuento(BigDecimal cuotaMensual, int descuentoPct) {
        if (descuentoPct == 0) {
            return cuotaMensual;
        }
        BigDecimal descuento = cuotaMensual.multiply(BigDecimal.valueOf(descuentoPct))
                .divide(BigDecimal.valueOf(100));
        return cuotaMensual.subtract(descuento);
    }
}
