package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CuotaMensualRepository extends JpaRepository<CuotaMensual, UUID> {

    List<CuotaMensual> findByContratoId(UUID contratoId);

    @Query("SELECT c FROM CuotaMensual c WHERE c.contrato.tenant.id = :tenantId AND c.estado = :estado")
    List<CuotaMensual> findByTenantIdAndEstado(@Param("tenantId") UUID tenantId,
                                                @Param("estado") EstadoCuota estado);

    @Query("SELECT c FROM CuotaMensual c WHERE c.contrato.tenant.id = :tenantId AND c.estado IN :estados")
    List<CuotaMensual> findByTenantIdAndEstadoIn(@Param("tenantId") UUID tenantId,
                                                  @Param("estados") List<EstadoCuota> estados);

    @Query("SELECT c FROM CuotaMensual c WHERE c.estado = 'PENDIENTE' AND c.fechaLimite <= :overdueDate")
    List<CuotaMensual> findPendienteBeforeDate(@Param("overdueDate") LocalDate overdueDate, Pageable pageable);
}
