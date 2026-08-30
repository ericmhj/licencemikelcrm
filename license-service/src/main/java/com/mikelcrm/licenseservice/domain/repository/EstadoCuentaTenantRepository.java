package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.EstadoCuentaTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstadoCuentaTenantRepository extends JpaRepository<EstadoCuentaTenant, UUID> {

    Page<EstadoCuentaTenant> findByTenantIdOrderByRegistradoEnDesc(UUID tenantId, Pageable pageable);

    /** Último movimiento registrado (para obtener saldo actual) */
    Optional<EstadoCuentaTenant> findFirstByTenantIdOrderByRegistradoEnDesc(UUID tenantId);

    /** Saldo actual del tenant (último saldo resultante) */
    @Query("SELECT e.saldoResultante FROM EstadoCuentaTenant e WHERE e.tenant.id = :tenantId ORDER BY e.registradoEn DESC LIMIT 1")
    Optional<BigDecimal> findSaldoActual(@Param("tenantId") UUID tenantId);

    /** Idempotencia: verifica si ya existe un movimiento con la clave de rastreo dada */
    boolean existsByClaveRastreo(String claveRastreo);
}
