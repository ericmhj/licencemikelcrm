package com.mikelcrm.licenseservice.security.rbac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare required permission for an endpoint method.
 * The RBAC interceptor uses this to evaluate access before handler execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * The resource being accessed (e.g., "contratos", "creditos", "tickets").
     */
    String recurso();

    /**
     * The action being performed on the resource (e.g., "leer", "crear", "actualizar").
     */
    String accion();
}
