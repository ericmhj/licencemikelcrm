package com.mikelcrm.licenseservice.startup;

import com.mikelcrm.licenseservice.service.command.CarteraAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runner que sincroniza el estado de cuenta con los movimientos de cartera
 * existentes cada vez que arranca el backend.
 *
 * Reconcilia todas las recargas (COMPRA) que aún no tienen su abono
 * correspondiente en el estado de cuenta. Es idempotente: usa el ID del evento
 * de crédito como clave de rastreo, por lo que ejecutarlo en cada arranque no
 * genera duplicados — solo crea los abonos faltantes.
 *
 * Se puede desactivar con la propiedad:
 *   license-service.reconciliation.on-startup=false
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class EstadoCuentaReconciliationRunner implements ApplicationRunner {

    private final CarteraAdminService carteraAdminService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Startup] Iniciando reconciliación automática cartera → estado de cuenta...");
        try {
            int creados = carteraAdminService.reconciliarTodos();
            log.info("[Startup] Reconciliación automática finalizada. Abonos creados: {}", creados);
        } catch (Exception e) {
            // No debe impedir el arranque de la aplicación
            log.error("[Startup] Error durante la reconciliación automática: {}", e.getMessage(), e);
        }
    }
}
