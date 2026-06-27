package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaqueteCreditosRepository extends JpaRepository<PaqueteCreditos, UUID> {

    Optional<PaqueteCreditos> findByTenantIdAndEstado(UUID tenantId, EstadoPaquete estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaqueteCreditos p WHERE p.tenant.id = :tenantId AND p.estado = 'ACTIVE'")
    Optional<PaqueteCreditos> findActiveByTenantForUpdate(@Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM PaqueteCreditos p WHERE p.estado = 'ACTIVE' AND p.fechaVencimiento = :fecha")
    List<PaqueteCreditos> findActiveByFechaVencimiento(@Param("fecha") LocalDate fecha, Pageable pageable);
}
