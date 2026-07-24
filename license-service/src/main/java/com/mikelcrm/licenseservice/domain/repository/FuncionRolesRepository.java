package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.FuncionRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuncionRolesRepository extends JpaRepository<FuncionRoles, UUID> {
    Optional<FuncionRoles> findByCodigo(String codigo);
}
