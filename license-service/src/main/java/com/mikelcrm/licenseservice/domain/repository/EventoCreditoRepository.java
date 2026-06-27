package com.mikelcrm.licenseservice.domain.repository;

import com.mikelcrm.licenseservice.domain.entity.EventoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventoCreditoRepository extends JpaRepository<EventoCredito, UUID> {
}
