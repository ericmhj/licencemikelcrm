# SDD — Spec-Driven Development Template
**Proyecto:** `[Nombre del proyecto]`  
**Feature / Módulo:** `[Nombre del feature o componente]`  
**Versión:** `0.1 — Draft`  
**Autor(es):** `[Nombre]`  
**Revisores:** `[Nombres]`  
**Fecha:** `YYYY-MM-DD`  
**Estado:** `Draft | In Review | Approved | Implemented`

---

## 0. Change Log

| Versión | Fecha | Autor | Cambio |
|---------|-------|-------|--------|
| 0.1 | YYYY-MM-DD | — | Draft inicial |

---

## 1. Resumen Ejecutivo

> _Una sola párrafo. Qué se construye, por qué ahora, cuál es el resultado esperado._

**Problema:** `[Descripción concisa del dolor o gap actual]`  
**Solución propuesta:** `[Qué construiremos]`  
**Impacto esperado:** `[KPI o métrica que moverá]`

---

## 2. Objetivos y Alcance

### 2.1 En Scope

- `[Feature 1]`
- `[Feature 2]`
- `[Feature 3]`

### 2.2 Fuera de Scope (Non-Goals)

- `[Lo que deliberadamente NO cubre este spec]`
- `[Funcionalidad diferida a una fase posterior]`

### 2.3 Criterios de Éxito

| Métrica | Baseline | Target | Cómo se mide |
|---------|----------|--------|--------------|
| `[Ej: Tiempo de activación de licencia]` | `[X seg]` | `[Y seg]` | `[Log / APM]` |
| `[Ej: Tasa de error en renovación]` | `[X %]` | `[Y %]` | `[Alerta en monitoreo]` |

---

## 3. Contexto y Antecedentes

### 3.1 Estado Actual del Sistema

> _Describe brevemente cómo funciona HOY el área afectada. Incluye deuda técnica relevante._

### 3.2 Decisiones Previas Relevantes

> _Links a otros SDDs, ADRs, tickets o decisiones que condicionan este diseño._

- `[Link o referencia]`

### 3.3 Restricciones

| Tipo | Descripción |
|------|-------------|
| Técnica | `[Ej: Debe integrarse con CRM X sin cambiar su esquema]` |
| Negocio | `[Ej: Cumplir regulación GDPR / SOC2]` |
| Tiempo | `[Ej: Go-live antes de Q3]` |
| Presupuesto | `[Ej: Sin licencias adicionales de terceros]` |

---

## 4. Modelo de Datos

### 4.1 Entidades Principales

```
[Diagrama ERD en texto o Mermaid]

erDiagram
    LICENCIA {
        uuid id PK
        string tipo
        date fecha_inicio
        date fecha_vencimiento
        string estado
        uuid cliente_id FK
        uuid plan_id FK
    }
    CLIENTE ||--o{ LICENCIA : tiene
    PLAN ||--o{ LICENCIA : define
```

### 4.2 Definición de Campos Críticos

| Entidad | Campo | Tipo | Nullable | Descripción | Reglas de negocio |
|---------|-------|------|----------|-------------|-------------------|
| Licencia | `estado` | enum | No | Estado actual | `activa`, `suspendida`, `expirada`, `cancelada` |
| Licencia | `tipo` | enum | No | Categoría | `trial`, `starter`, `pro`, `enterprise` |
| Licencia | `seats` | int | No | Usuarios permitidos | > 0; no puede reducirse por debajo del uso actual |

### 4.3 Máquina de Estados

```
[Estado de la licencia]

CREADA → TRIAL → ACTIVA → SUSPENDIDA → CANCELADA
                    ↓                       ↑
                 EXPIRADA ────────────────→ ─
```

| Transición | Trigger | Condición | Side Effect |
|------------|---------|-----------|-------------|
| `CREADA → TRIAL` | Registro de cliente | — | Enviar email bienvenida |
| `TRIAL → ACTIVA` | Pago exitoso | Plan seleccionado | Provisionar accesos |
| `ACTIVA → SUSPENDIDA` | Pago fallido | 3 intentos fallidos | Bloquear acceso, notificar |
| `ACTIVA → EXPIRADA` | Cron diario | `fecha_vencimiento < hoy` | Revocar tokens |
| `SUSPENDIDA → ACTIVA` | Pago exitoso | — | Restaurar accesos |
| `EXPIRADA → CANCELADA` | 30 días sin renovación | — | Purge de datos según política |

---

## 5. Especificación de API

### 5.1 Endpoints

#### `POST /api/v1/licencias`
Crea una nueva licencia.

**Request Body:**
```json
{
  "cliente_id": "uuid",
  "plan_id": "uuid",
  "tipo": "pro",
  "seats": 10,
  "fecha_inicio": "2026-07-01"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "estado": "activa",
  "fecha_vencimiento": "2027-07-01",
  "activation_token": "string"
}
```

**Errores:**
| Código | Caso |
|--------|------|
| 400 | Campos requeridos faltantes o inválidos |
| 409 | El cliente ya tiene una licencia activa del mismo tipo |
| 422 | `seats` < 1 o plan no permite el tipo solicitado |

---

#### `PATCH /api/v1/licencias/{id}/estado`
Cambia el estado de una licencia.

**Request Body:**
```json
{
  "estado": "suspendida",
  "motivo": "pago_fallido"
}
```

**Response 200:** Objeto licencia actualizado.

---

### 5.2 Webhooks / Eventos

| Evento | Trigger | Payload clave | Consumidores |
|--------|---------|--------------|--------------|
| `licencia.activada` | Transición → ACTIVA | `id`, `cliente_id`, `fecha_vencimiento` | CRM, Email, Billing |
| `licencia.expirada` | Cron diario | `id`, `cliente_id` | CRM, Accesos |
| `licencia.renovada` | Pago exitoso + extensión | `id`, `nueva_fecha_vencimiento` | CRM, Email |

---

## 6. Reglas de Negocio

> _Lista numerada. Cada regla debe ser testeable y tener un owner._

| ID | Regla | Owner | Test ID |
|----|-------|-------|---------|
| RN-01 | Un cliente no puede tener más de una licencia ACTIVA del mismo `tipo` simultáneamente | Backend | TC-01 |
| RN-02 | `seats` no puede reducirse por debajo de la cantidad de usuarios actualmente asignados | Backend | TC-02 |
| RN-03 | La renovación automática se intenta 7, 3 y 1 día(s) antes del vencimiento | Billing | TC-03 |
| RN-04 | Una licencia TRIAL dura máximo 14 días calendario, sin extensión | Backend | TC-04 |
| RN-05 | Al cancelar, los datos del cliente se retienen 90 días antes de purge | Data | TC-05 |

---

## 7. Flujos de Usuario (User Flows)

### 7.1 Activación de Licencia Nueva

```
Cliente → Selecciona plan → Ingresa pago
    → [Pago OK?]
        SÍ → POST /licencias → Email bienvenida → Acceso habilitado
        NO → Mostrar error → Reintentar (max 3)
```

### 7.2 Renovación Automática

```
Cron T-7d → Intento de cobro
    → [Cobro OK?]
        SÍ → Extender fecha_vencimiento → Evento licencia.renovada
        NO → Reintentar T-3d, T-1d
            → 3 fallos → SUSPENDIDA → Notificación crítica al cliente
```

### 7.3 Upgrade de Plan

```
Admin CRM → Selecciona nueva licencia → Valida RN-01, RN-02
    → [Válido?]
        SÍ → PATCH /licencias/{id} → Prorratear cobro → Provisionar nuevos permisos
        NO → Retornar error con detalle
```

---

## 8. Arquitectura Técnica

### 8.1 Componentes Afectados

| Componente | Rol | Cambio requerido |
|------------|-----|-----------------|
| `licencias-service` | Gestión de ciclo de vida | Nuevo módulo |
| `billing-service` | Cobros y renovación | Consumir eventos `licencia.*` |
| `auth-service` | Control de acceso | Validar estado de licencia en cada request |
| `cron-jobs` | Expiración y avisos | Nuevo job `check-expiring-licenses` |
| `notification-service` | Emails y alertas | Nuevas plantillas de licencia |

### 8.2 Dependencias Externas

| Servicio | Uso | Alternativa si falla |
|----------|-----|----------------------|
| `[Ej: Stripe]` | Cobros recurrentes | Marcar para reintentar; no suspender inmediatamente |
| `[Ej: SendGrid]` | Notificaciones email | Cola de reintentos; log de fallos |

### 8.3 Decisiones de Diseño (ADRs)

| # | Decisión | Alternativa descartada | Razón |
|---|----------|----------------------|-------|
| ADR-01 | Estado de licencia en BD, no solo en caché | Solo Redis | Consistencia ante restart |
| ADR-02 | Webhook en lugar de polling | Polling cada 5 min | Reduce carga, más real-time |

---

## 9. Seguridad y Compliance

| Área | Requisito | Implementación |
|------|-----------|----------------|
| Autenticación | Solo tokens válidos acceden a `/licencias` | JWT con claim `licencia_id` |
| Autorización | Admin CRM puede modificar cualquier licencia; cliente solo la propia | RBAC middleware |
| Auditoría | Toda transición de estado debe quedar en log inmutable | Append-only `auditoria_licencias` table |
| Datos sensibles | Datos de pago NO se almacenan localmente | Delegado 100% al proveedor de billing |
| GDPR / privacidad | Purge de datos tras cancelación (90 días) | Job de limpieza + confirmación |

---

## 10. Rendimiento y Escalabilidad

| Escenario | Volumen esperado | SLA | Estrategia |
|-----------|-----------------|-----|------------|
| Validación de licencia en cada request | 500 req/s pico | < 10 ms p99 | Cache en Redis con TTL 5 min |
| Cron de expiración diario | Hasta 10,000 licencias | < 5 min total | Batch de 500 con backoff |
| Renovación masiva (fin de mes) | 2,000 cobros simultáneos | < 30 seg | Queue con concurrencia controlada |

---

## 11. Plan de Pruebas

### 11.1 Casos de Prueba Clave

| ID | Escenario | Precondición | Resultado Esperado |
|----|-----------|-------------|-------------------|
| TC-01 | Crear segunda licencia activa del mismo tipo | Cliente con licencia ACTIVA tipo `pro` | HTTP 409 |
| TC-02 | Reducir seats por debajo de uso actual | 8 users activos, intentar reducir a 5 | HTTP 422 con detalle |
| TC-03 | Renovación automática exitosa T-7 | Licencia vence en 7 días, tarjeta válida | Fecha extendida, evento emitido |
| TC-04 | Trial expirado automáticamente | Trial creado hace 15 días | Estado = `expirada` |
| TC-05 | Purge de datos post-cancelación | Licencia cancelada hace 91 días | Datos eliminados; audit log intacto |

### 11.2 Estrategia de Testing

| Tipo | Cobertura mínima | Herramienta |
|------|-----------------|-------------|
| Unit tests | Todas las reglas de negocio (RN-*) | Jest / pytest |
| Integration tests | Flujos end-to-end de activación y renovación | Supertest / Postman |
| Contract tests | Payloads de webhooks | Pact |
| Load tests | Escenarios de pico (sección 10) | k6 |

---

## 12. Plan de Implementación

| Fase | Entregable | Estimación | Dependencias |
|------|-----------|-----------|--------------|
| 1 — Data Model | Migraciones + entidades | 2 días | — |
| 2 — Core API | CRUD + máquina de estados | 3 días | Fase 1 |
| 3 — Billing Integration | Webhooks de pago → transiciones | 3 días | Fase 2 |
| 4 — Cron Jobs | Expiración y avisos | 2 días | Fase 2 |
| 5 — Notificaciones | Emails por evento | 1 día | Fase 3, 4 |
| 6 — QA & Hardening | Tests, load test, fix | 3 días | Fases 1-5 |

**Total estimado:** ~14 días de desarrollo.

---

## 13. Preguntas Abiertas y Decisiones Pendientes

| # | Pregunta | Owner | Deadline | Estado |
|---|----------|-------|----------|--------|
| Q-01 | ¿Qué pasa con licencias enterprise con contrato anual prepagado en renovación fallida? | Product | YYYY-MM-DD | Abierta |
| Q-02 | ¿El upgrade de plan genera una nueva licencia o modifica la existente? | Arquitectura | YYYY-MM-DD | Abierta |
| Q-03 | ¿Cuántos reintentos de cobro antes de suspensión? ¿Con qué frecuencia? | Billing | YYYY-MM-DD | Abierta |

---

## 14. Glosario

| Término | Definición |
|---------|-----------|
| Licencia | Contrato digital que otorga acceso a features de la plataforma por un período |
| Seat | Unidad de usuario autorizado bajo una licencia |
| Trial | Licencia temporal gratuita de evaluación |
| Renovación automática | Proceso de cobro y extensión de licencia sin intervención manual |
| Provisioning | Habilitación técnica de accesos y permisos tras activación de licencia |

---

## 15. Anexos

- [ ] Diagrama de arquitectura (link)
- [ ] Mockups / Wireframes (link)
- [ ] ADR completos (link)
- [ ] Documento de integración con billing (link)

---

> **Checklist de aprobación:**
> - [ ] Product Owner revisó y aprobó alcance
> - [ ] Tech Lead revisó arquitectura
> - [ ] QA revisó plan de pruebas
> - [ ] Security revisó sección 9
> - [ ] Estimación validada por el equipo
