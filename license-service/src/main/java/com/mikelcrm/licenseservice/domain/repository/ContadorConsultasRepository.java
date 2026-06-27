package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.ContadorConsultas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContadorConsultasRepository extends JpaRepository<ContadorConsultas, UUID> {

    List<ContadorConsultas> findByTenantId(UUID tenantId);

    Optional<ContadorConsultas> findByTenantIdAndTipoReporteAndPeriodoAnio(
            UUID tenantId, String tipoReporte, int periodoAnio);

    @Query("SELECT COALESCE(SUM(c.totalConsultas), 0) FROM ContadorConsultas c WHERE c.tenant.id = :tenantId AND c.periodoAnio = :periodoAnio")
    int sumTotalConsultasByTenantIdAndPeriodoAnio(@Param("tenantId") UUID tenantId,
                                                   @Param("periodoAnio") int periodoAnio);
}
