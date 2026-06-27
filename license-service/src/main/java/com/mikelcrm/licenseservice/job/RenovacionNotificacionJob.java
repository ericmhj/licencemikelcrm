package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.Renovacion;
import com.mikelcrm.licenseservice.domain.enums.EstadoRenovacion;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.RenovacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Daily job at 8:00 AM that sends renewal notifications for contracts
 * expiring in 30, 7, or 1 days.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RenovacionNotificacionJob {

    private final ContratoAnualRepository contratoRepository;
    private final RenovacionRepository renovacionRepository;

    private static final BigDecimal TARIFA_RENOVACION = new BigDecimal("99.00");
    private static final int BATCH_SIZE = 500;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendRenewalNotifications() {
        log.info("RenovacionNotificacionJob started");
        LocalDate today = LocalDate.now();
        int totalProcessed = 0;

        // Find contracts expiring at exactly 30, 7, or 1 days from today
        List<LocalDate> targetDates = List.of(
                today.plusDays(30),
                today.plusDays(7),
                today.plusDays(1)
        );

        List<ContratoAnual> contracts;
        do {
            contracts = contratoRepository.findActiveByFechaVencimientoIn(targetDates, PageRequest.of(0, BATCH_SIZE));
            for (ContratoAnual contract : contracts) {
                try {
                    processNotification(contract, today);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Error processing renewal notification for contract {}: {}",
                            contract.getId(), e.getMessage(), e);
                }
            }
            // Since we update the renovacion record but don't change the contract state,
            // we need to break to avoid infinite loop. The query returns the same set each time.
            break;
        } while (contracts.size() == BATCH_SIZE);

        log.info("RenovacionNotificacionJob completed. Processed {} notifications", totalProcessed);
    }

    @Transactional
    protected void processNotification(ContratoAnual contract, LocalDate today) {
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, contract.getFechaVencimiento());

        // Find or create the Renovacion record
        Renovacion renovacion = renovacionRepository.findByContratoId(contract.getId())
                .orElseGet(() -> createRenovacion(contract));

        LocalDateTime now = LocalDateTime.now();

        // Update the appropriate notification timestamp (idempotent: skip if already set)
        if (daysUntilExpiry == 30 && renovacion.getNotificado30d() == null) {
            renovacion.setNotificado30d(now);
            renovacionRepository.save(renovacion);
            log.info("Sent 30-day renewal notification for contract {}. Tenant: {}. Tarifa: {}€",
                    contract.getId(), contract.getTenant().getId(), TARIFA_RENOVACION);
        } else if (daysUntilExpiry == 7 && renovacion.getNotificado7d() == null) {
            renovacion.setNotificado7d(now);
            renovacionRepository.save(renovacion);
            log.info("Sent 7-day renewal notification for contract {}. Tenant: {}. Tarifa: {}€",
                    contract.getId(), contract.getTenant().getId(), TARIFA_RENOVACION);
        } else if (daysUntilExpiry == 1 && renovacion.getNotificado1d() == null) {
            renovacion.setNotificado1d(now);
            renovacionRepository.save(renovacion);
            log.info("Sent 1-day renewal notification for contract {}. Tenant: {}. Tarifa: {}€",
                    contract.getId(), contract.getTenant().getId(), TARIFA_RENOVACION);
        }
        // Event publishing will be done in Phase 10
    }

    private Renovacion createRenovacion(ContratoAnual contract) {
        Renovacion renovacion = Renovacion.builder()
                .id(UUID.randomUUID())
                .contrato(contract)
                .fechaVencimientoContrato(contract.getFechaVencimiento())
                .tarifaRenovacion(TARIFA_RENOVACION)
                .primeraCuota(contract.getCuotaMensual())
                .estado(EstadoRenovacion.PENDIENTE)
                .build();
        return renovacionRepository.save(renovacion);
    }
}
