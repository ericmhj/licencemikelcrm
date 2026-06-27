package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.enums.EstadoContrato;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContratoAnualRepository extends JpaRepository<ContratoAnual, UUID> {

    List<ContratoAnual> findByTenantId(UUID tenantId);

    List<ContratoAnual> findByTenantIdAndEstado(UUID tenantId, EstadoContrato estado);

    @Query("SELECT c FROM ContratoAnual c WHERE c.tenant.id = :tenantId AND c.estado IN :estados")
    List<ContratoAnual> findByTenantIdAndEstadoIn(@Param("tenantId") UUID tenantId,
                                                   @Param("estados") List<EstadoContrato> estados);

    @Query("SELECT c FROM ContratoAnual c WHERE c.estado = 'ACTIVE' AND c.fechaVencimiento = :fecha")
    List<ContratoAnual> findActiveByFechaVencimiento(@Param("fecha") LocalDate fecha, Pageable pageable);

    @Query("SELECT c FROM ContratoAnual c WHERE c.estado = 'ACTIVE' AND c.fechaVencimiento = :fecha AND c.renovacionAuto = true")
    List<ContratoAnual> findActiveAutoRenewByFechaVencimiento(@Param("fecha") LocalDate fecha, Pageable pageable);

    @Query("SELECT c FROM ContratoAnual c WHERE c.estado = 'ACTIVE' AND c.fechaVencimiento IN :fechas")
    List<ContratoAnual> findActiveByFechaVencimientoIn(@Param("fechas") List<LocalDate> fechas, Pageable pageable);
}
