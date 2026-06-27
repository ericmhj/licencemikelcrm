package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.ContadorConsultas;
import com.mikelcrm.licenseservice.domain.entity.EventoCredito;
import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.TipoEventoCredito;
import com.mikelcrm.licenseservice.domain.repository.ContadorConsultasRepository;
import com.mikelcrm.licenseservice.domain.repository.EventoCreditoRepository;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.dto.RegisterConsultationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final ContadorConsultasRepository contadorRepository;
    private final PaqueteCreditosRepository paqueteRepository;
    private final EventoCreditoRepository eventoCreditoRepository;
    private final TenantRepository tenantRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final String GLOBAL_COUNTER_TYPE = "__GLOBAL__";

    /**
     * Task 7.9: Register a consultation and handle excess billing.
     * Increments global and per-type counters. If global counter exceeds threshold,
     * deducts 1 credit per complete block of 100 excess consultations.
     *
     * @return the new global consultation count
     */
    @Transactional
    public int registerConsultation(UUID tenantId, RegisterConsultationRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        int currentYear = Year.now().getValue();

        // Increment per-type counter
        ContadorConsultas tipoCounter = findOrCreateCounter(tenant, request.getTipoReporte(), currentYear);
        tipoCounter.setTotalConsultas(tipoCounter.getTotalConsultas() + 1);
        tipoCounter.setUltimoIncremento(LocalDateTime.now());
        contadorRepository.save(tipoCounter);

        // Increment global counter
        ContadorConsultas globalCounter = findOrCreateCounter(tenant, GLOBAL_COUNTER_TYPE, currentYear);
        globalCounter.setTotalConsultas(globalCounter.getTotalConsultas() + 1);
        globalCounter.setUltimoIncremento(LocalDateTime.now());
        contadorRepository.save(globalCounter);

        int globalCount = globalCounter.getTotalConsultas();

        // Calculate threshold and check excess
        PaqueteCreditos paquete = paqueteRepository.findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE)
                .orElse(null);

        if (paquete != null) {
            int umbral = paquete.getCreditosTotalesAdquiridos() * 100;
            int excedente = globalCount - umbral;

            if (excedente > 0 && excedente % 100 == 0) {
                // Deduct 1 credit for the completed block of 100 excess consultations
                BigDecimal costoExcedente = BigDecimal.ONE;

                if (paquete.getSaldoDisponible().compareTo(costoExcedente) >= 0) {
                    // Deduct from saldo
                    BigDecimal nuevoSaldo = paquete.getSaldoDisponible().subtract(costoExcedente);
                    paquete.setSaldoDisponible(nuevoSaldo);
                    paqueteRepository.save(paquete);

                    EventoCredito evento = EventoCredito.builder()
                            .id(UUID.randomUUID())
                            .tenant(tenant)
                            .paquete(paquete)
                            .tipo(TipoEventoCredito.EXCEDENTE)
                            .cantidad(costoExcedente.negate())
                            .saldoResultante(nuevoSaldo)
                            .documentoId(request.getDocumentoId())
                            .usuarioId(request.getUsuarioId())
                            .ocurridoEn(LocalDateTime.now())
                            .build();
                    eventoCreditoRepository.save(evento);

                    log.info("Excess charge: deducted 1 credit for tenant {}. Global count: {}, Threshold: {}, New balance: {}",
                            tenantId, globalCount, umbral, nuevoSaldo);
                } else {
                    // Saldo is 0: register debt but don't block consultations (RN-CRED-10)
                    EventoCredito evento = EventoCredito.builder()
                            .id(UUID.randomUUID())
                            .tenant(tenant)
                            .paquete(paquete)
                            .tipo(TipoEventoCredito.EXCEDENTE)
                            .cantidad(costoExcedente.negate())
                            .saldoResultante(paquete.getSaldoDisponible())
                            .documentoId(request.getDocumentoId())
                            .usuarioId(request.getUsuarioId())
                            .ocurridoEn(LocalDateTime.now())
                            .build();
                    eventoCreditoRepository.save(evento);

                    log.warn("Excess charge: saldo is 0 for tenant {}. Debt registered. Global count: {}, Threshold: {}",
                            tenantId, globalCount, umbral);
                }

                // Publish consulta.excedente event
                Map<String, Object> eventPayload = new HashMap<>();
                eventPayload.put("tenantId", tenantId.toString());
                eventPayload.put("consultaNum", globalCount);
                eventPayload.put("creditoCobrado", costoExcedente.toPlainString());
                domainEventPublisher.publish("consulta.excedente", tenantId, eventPayload, null);

                cacheInvalidationService.invalidateAccessCache(tenantId);
            }
        }

        return globalCount;
    }

    private ContadorConsultas findOrCreateCounter(Tenant tenant, String tipoReporte, int periodoAnio) {
        return contadorRepository.findByTenantIdAndTipoReporteAndPeriodoAnio(
                tenant.getId(), tipoReporte, periodoAnio)
                .orElseGet(() -> {
                    ContadorConsultas newCounter = ContadorConsultas.builder()
                            .id(UUID.randomUUID())
                            .tenant(tenant)
                            .tipoReporte(tipoReporte)
                            .totalConsultas(0)
                            .periodoAnio(periodoAnio)
                            .ultimoIncremento(LocalDateTime.now())
                            .build();
                    return contadorRepository.save(newCounter);
                });
    }
}
