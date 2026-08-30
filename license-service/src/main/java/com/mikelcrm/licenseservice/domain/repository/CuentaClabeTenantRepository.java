package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.CuentaClabeTenant;
import com.mikelcrm.licenseservice.domain.enums.TipoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CuentaClabeTenantRepository extends JpaRepository<CuentaClabeTenant, UUID> {

    Optional<CuentaClabeTenant> findByClabe(String clabe);

    List<CuentaClabeTenant> findByTenantIdAndActivaTrue(UUID tenantId);

    Optional<CuentaClabeTenant> findByTenantIdAndTipoAndActivaTrue(UUID tenantId, TipoCobro tipo);
}
