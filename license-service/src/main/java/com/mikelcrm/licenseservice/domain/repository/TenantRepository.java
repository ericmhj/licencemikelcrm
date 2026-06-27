package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    List<Tenant> findByEstado(EstadoTenant estado, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE t.estado = :estado AND t.fechaSuspension <= :cutoffDate")
    List<Tenant> findByEstadoAndFechaSuspensionBefore(@Param("estado") EstadoTenant estado,
                                                      @Param("cutoffDate") LocalDateTime cutoffDate,
                                                      Pageable pageable);
}
