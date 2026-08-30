package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.CobroMensual;
import com.mikelcrm.licenseservice.domain.enums.EstadoCobroMensual;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CobroMensualRepository extends JpaRepository<CobroMensual, UUID> {

    Optional<CobroMensual> findByTenantIdAndPeriodoMes(UUID tenantId, LocalDate periodoMes);

    List<CobroMensual> findByTenantIdOrderByPeriodoMesDesc(UUID tenantId);

    @Query("SELECT c FROM CobroMensual c WHERE c.estado = :estado AND c.fechaVencimiento < :fecha")
    List<CobroMensual> findVencidos(@Param("estado") EstadoCobroMensual estado,
                                    @Param("fecha") LocalDate fecha,
                                    Pageable pageable);

    /** Busca el cobro PENDIENTE más reciente de un tenant para aplicar pago */
    Optional<CobroMensual> findFirstByTenantIdAndEstadoOrderByPeriodoMesDesc(
            UUID tenantId, EstadoCobroMensual estado);
}
