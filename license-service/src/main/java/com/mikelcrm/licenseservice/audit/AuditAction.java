package com.mikelcrm.licenseservice.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 * The AuditAspect intercepts annotated methods and creates an immutable audit log entry
 * with before/after state, user info, and result.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    /**
     * The action being performed (e.g., "crear_contrato", "pago_cuota", "consumir_credito").
     */
    String accion();

    /**
     * The entity type being acted upon (e.g., "contrato", "cuota", "credito", "tenant").
     */
    String entidad();
}
