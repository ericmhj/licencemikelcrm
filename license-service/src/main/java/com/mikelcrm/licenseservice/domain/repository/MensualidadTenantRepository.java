package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.MensualidadTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MensualidadTenantRepository extends JpaRepository<MensualidadTenant, UUID> {

    /** Mensualidades PENDIENTE del tenant, de la más antigua a la más reciente. */
    @Query("SELECT m FROM MensualidadTenant m WHERE m.tenant.id = :tenantId " +
           "AND m.estado = 'PENDIENTE' ORDER BY m.periodoMes ASC")
    List<MensualidadTenant> findPendientesOrdenadas(@Param("tenantId") UUID tenantId);

    /** Última mensualidad registrada (por periodo) del tenant, exista o no. */
    @Query("SELECT m FROM MensualidadTenant m WHERE m.tenant.id = :tenantId " +
           "ORDER BY m.periodoMes DESC")
    List<MensualidadTenant> findByTenantOrderByPeriodoDesc(@Param("tenantId") UUID tenantId);

    Optional<MensualidadTenant> findByTenantIdAndPeriodoMes(UUID tenantId, LocalDate periodoMes);

    boolean existsByTenantIdAndPeriodoMes(UUID tenantId, LocalDate periodoMes);
}
