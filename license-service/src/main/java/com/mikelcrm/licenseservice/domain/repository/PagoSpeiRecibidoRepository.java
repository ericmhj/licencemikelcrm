package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.PagoSpeiRecibido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoSpeiRecibidoRepository extends JpaRepository<PagoSpeiRecibido, UUID> {

    boolean existsByClaveRastreo(String claveRastreo);

    Optional<PagoSpeiRecibido> findByClaveRastreo(String claveRastreo);

    List<PagoSpeiRecibido> findByTenantIdOrderByRecibidoEnDesc(UUID tenantId);
}
