package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.Renovacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RenovacionRepository extends JpaRepository<Renovacion, UUID> {

    Optional<Renovacion> findByContratoId(UUID contratoId);
}
