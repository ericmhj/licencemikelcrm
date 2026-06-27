package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.CuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuotaAlmacenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CuotaAlmacenamientoRepository extends JpaRepository<CuotaAlmacenamiento, UUID> {

    List<CuotaAlmacenamiento> findByTenantIdAndEstado(UUID tenantId, EstadoCuotaAlmacenamiento estado);

    List<CuotaAlmacenamiento> findByTenantId(UUID tenantId);
}
