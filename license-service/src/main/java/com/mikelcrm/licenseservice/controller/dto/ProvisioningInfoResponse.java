package com.mikelcrm.licenseservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos mínimos que SMT necesita para materializar (provisionar) un tenant en
 * su propio sistema, tomándolos de la fuente de verdad (license-service).
 * <p>
 * Se usa en la reconciliación server-to-server: SMT consulta este objeto y
 * ejecuta su provisionamiento idempotente (schema + public.tenants + usuario
 * admin en Keycloak), sin depender de Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisioningInfoResponse {
    /** UUID del tenant en license-service (clave de correlación cross-service). */
    private String tenantId;
    /** Slug derivado del nombre (coincide con el schema sgr_{slug} en SMT). */
    private String slug;
    /** Nombre del tenant. */
    private String nombre;
    /** Código del plan (o null si no tiene plan asignado). */
    private String planCodigo;
    /** Estado del tenant (ONBOARDING/ACTIVE/SUSPENDED/CANCELLED). */
    private String estado;
    /** Email del administrador del tenant (usuario admin en Keycloak). */
    private String adminEmail;
}
