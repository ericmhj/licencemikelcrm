package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.RolUsuario;
import com.mikelcrm.licenseservice.domain.entity.RolUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolUsuarioRepository extends JpaRepository<RolUsuario, RolUsuarioId> {

    Optional<RolUsuario> findByUsuarioIdAndTenantId(UUID usuarioId, UUID tenantId);
}
