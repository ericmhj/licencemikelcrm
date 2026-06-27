# SDD — Microservicio de Gestión de Licencias y Control de Acceso
**Proyecto:** `Mikel CRM — License Service`
**Módulo:** `license-service` (Microservicio independiente)
**Versión:** `1.0 — Draft`
**Stack:** `Java 21 · Spring Boot 3.x · PostgreSQL 16 · Redis 7`
**Autor:** `Analista de Sistemas — LicenciasCRMMikel`
**Fecha:** `2026-06-20`
**Estado:** `Draft`

---

## 0. Change Log

| Versión | Fecha | Autor | Cambio |
|---------|-------|-------|--------|
| 1.0 | 2026-06-20 | Analista de Sistemas | Draft inicial — arquitectura CQRS + ciclo de vida completo de Tenant |

---

## 1. Resumen Ejecutivo

**Problema:** El control de acceso y la gestión de licencias es el componente más consultado del SaaS — se invoca en **cada acción** de cada usuario de cada Tenant. Implementarlo dentro del monolito principal generaría un cuello de botella de latencia, un punto único de fallo y una deuda de escalabilidad estructural.

**Solución propuesta:** Microservicio independiente `license-service` con arquitectura **CQRS + caché Redis** que separa el camino de escritura (ciclo de vida de contratos, cobros, reglas de negocio) del camino de lectura (validación ultrarrápida de acceso). El SaaS Core consulta únicamente el caché Redis; la base de datos relacional es la fuente de verdad y se actualiza de forma asíncrona.

**Impacto esperado:** Validación de licencia en < 5 ms p99; disponibilidad del 99.9 % independiente del estado del monolito; escalabilidad horizontal del servicio de lectura sin afectar la lógica de negocio.

---

## 2. Objetivos y Alcance

### 2.1 En Scope

- Ciclo de vida completo del Tenant: onboarding → contrato anual → cuotas mensuales → renovación → suspensión → reactivación → cancelación
- Motor de reglas de negocio: descuentos por puntualidad, Cuota de Almacenamiento, créditos, contadores de consultas
- API de Commands (escritura) y API de Queries (lectura / validación de acceso)
- Estrategia de caché Redis con TTL e invalidación por eventos
- Publicación de Domain Events hacia el bus de mensajería (Kafka / RabbitMQ)
- RBAC: gestión de roles y permisos por Tenant
- Audit Log inmutable de todas las transiciones de estado

### 2.2 Fuera de Scope

- Procesamiento de pagos (delegado al Payment Service; este servicio consume eventos de pago)
- Envío de notificaciones (delegado al Notification Service; este servicio publica eventos)
- Lógica de negocio del CRM core (tickets, reportes, eFirma)
- Frontend / UI (este documento cubre solo el backend del microservicio)

### 2.3 Criterios de Éxito

| Métrica | Target | Medición |
|---------|--------|----------|
| Latencia de validación de acceso (Query) | < 5 ms p99 | APM — Redis hit rate |
| Latencia de Commands (escritura) | < 200 ms p99 | APM |
| Disponibilidad del servicio | ≥ 99.9 % mensual | Health checks + alertas |
| Cache hit rate | ≥ 95 % | Redis metrics |
| Cobertura de tests de reglas de negocio | 100 % de RN-* | JUnit + Jacoco |

---

## 3. Arquitectura General

### 3.1 Diagrama de Componentes

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            MIKEL SAAS PLATFORM                           │
│                                                                          │
│  ┌──────────────┐    (1) validate_access()       ┌────────────────────┐  │
│  │  SaaS Core   │ ─────────────────────────────► │   license-service  │  │
│  │  (monolito)  │ ◄─────────────────────────────  │   Query API        │  │
│  └──────────────┘    (2) {allowed: true, role}   └────────┬───────────┘  │
│                                                           │ (3) Redis hit │
│  ┌──────────────┐                                ┌────────▼───────────┐  │
│  │  Admin Panel │ ──── Commands ────────────────► │   license-service  │  │
│  │  (backoffice)│                                 │   Command API      │  │
│  └──────────────┘                                └────────┬───────────┘  │
│                                                           │              │
│  ┌──────────────┐    Domain Events                ┌───────▼────────────┐  │
│  │  Payment Svc │ ──── payment.confirmed ────────► │   Event Handler    │  │
│  │              │ ◄─── license.suspended ──────── │   (async)          │  │
│  └──────────────┘                                └────────┬───────────┘  │
│                                                           │              │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                     license-service internals                       │ │
│  │                                                                     │ │
│  │  ┌──────────────────┐   invalidate   ┌──────────────────────────┐  │ │
│  │  │  PostgreSQL 16   │ ─────────────► │  Redis 7 (cache)         │  │ │
│  │  │  (source of      │               │  TTL: 5 min               │  │ │
│  │  │   truth)         │ ◄───────────── │  Key: tenant:{id}:access  │  │ │
│  │  └──────────────────┘   async sync  └──────────────────────────┘  │ │
│  │                                                                     │ │
│  │  ┌──────────────────┐                ┌──────────────────────────┐  │ │
│  │  │  Kafka / RabbitMQ│                │  Audit Log (append-only) │  │ │
│  │  │  (event bus)     │                │  PostgreSQL partitioned  │  │ │
│  │  └──────────────────┘                └──────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Patrón CQRS — Separación de Responsabilidades

| Responsabilidad | Componente | Latencia objetivo | Persistencia |
|----------------|------------|-------------------|--------------|
| **Query** — validar acceso en cada acción del usuario | `AccessQueryService` | < 5 ms p99 | Redis (caché) |
| **Query** — leer estado de contrato / créditos (panel) | `ContractQueryService` | < 50 ms p99 | PostgreSQL read replica |
| **Command** — crear contrato, registrar pago, cambiar estado | `ContractCommandService` | < 200 ms p99 | PostgreSQL primary |
| **Command** — consumir crédito, cobrar excedente | `CreditCommandService` | < 100 ms p99 | PostgreSQL primary |
| **Event Handler** — reaccionar a eventos externos (pagos) | `PaymentEventHandler` | Asíncrono | PostgreSQL + Redis invalidation |

### 3.3 Estrategia de Caché Redis

```
Estructura de la clave principal:
  tenant:{tenantId}:access

Valor (JSON serializado):
{
  "tenantId": "uuid",
  "status": "ACTIVE | SUSPENDED | CANCELLED",
  "modules": ["ADMIN", "REPORTS", "TICKETS"],
  "creditBalance": 42,
  "roles": {
    "userId-1": "SUPERVISOR",
    "userId-2": "ASISTENTE"
  },
  "suspendedSince": null,
  "cachedAt": "ISO8601",
  "ttl": 300
}

TTL: 300 segundos (5 minutos)
Invalidación forzada: en cualquier Command que cambie el estado del Tenant
Warmup: al iniciar el servicio, pre-cargar Tenants ACTIVE con contratos próximos a vencer
```

**Flujo de validación (hot path — debe ser < 5 ms):**
```
validate_access(tenantId, userId, module, action)
  → GET Redis key tenant:{tenantId}:access
      → HIT  → deserialize → evaluar permisos → return {allowed, role}   [~1 ms]
      → MISS → query PostgreSQL → write Redis → evaluar → return          [~30 ms]
```

---

## 4. Modelo de Datos

### 4.1 Diagrama Entidad-Relación

```
erDiagram

  TENANT {
    uuid      id                PK
    string    nombre
    string    email_contacto
    string    estado            "ONBOARDING | ACTIVE | SUSPENDED | CANCELLED"
    string    modalidad_reporte "ESTANDAR | PERSONALIZADO"
    timestamp fecha_alta
    timestamp fecha_suspension
    timestamp fecha_cancelacion
    int       reportes_almacenados
    decimal   deuda_almacenamiento
  }

  CONTRATO_ANUAL {
    uuid      id                PK
    uuid      tenant_id         FK
    string    tipo              "MODULO | CREDITOS"
    string    modulo            "ADMIN | REPORTS | ... | null"
    string    estado            "ACTIVE | SUSPENDED | CANCELLED | EXPIRED"
    decimal   cuota_mensual
    int       cuotas_pagadas
    int       cuotas_totales    "siempre 12"
    date      fecha_inicio
    date      fecha_vencimiento
    date      fecha_aniversario "día del mes del cobro"
    bool      renovacion_auto
    timestamp creado_en
  }

  CUOTA_MENSUAL {
    uuid      id                PK
    uuid      contrato_id       FK
    int       numero            "1..12"
    date      fecha_limite
    decimal   monto_original
    decimal   monto_cobrado     "con descuento aplicado si aplica"
    decimal   descuento_pct     "10 | 3 | 0"
    string    estado            "PENDIENTE | PAGADA | VENCIDA | EN_MORA"
    timestamp fecha_pago
    int       intento_cobro     "1..3"
  }

  RENOVACION {
    uuid      id                PK
    uuid      contrato_id       FK
    date      fecha_vencimiento_contrato
    decimal   tarifa_renovacion "99 EUR fija"
    decimal   primera_cuota
    string    estado            "PENDIENTE | COMPLETADA | RECHAZADA | CANCELADA"
    timestamp notificado_30d
    timestamp notificado_7d
    timestamp notificado_1d
    timestamp procesada_en
  }

  CUOTA_ALMACENAMIENTO {
    uuid      id                PK
    uuid      tenant_id         FK
    date      periodo_mes
    int       reportes_snapshot "número de reportes al momento del cálculo"
    decimal   monto             "50 + CEIL(MAX(reportes-100,0)/100)*10"
    string    estado            "PENDIENTE | PAGADA"
    timestamp generada_en
  }

  PAQUETE_CREDITOS {
    uuid      id                PK
    uuid      tenant_id         FK
    uuid      contrato_id       FK
    int       creditos_paquete
    int       creditos_bonus    "siempre 4"
    int       saldo_disponible
    int       creditos_totales_adquiridos
    date      fecha_inicio
    date      fecha_vencimiento
    string    estado            "ACTIVE | EXPIRED | CANCELLED"
  }

  EVENTO_CREDITO {
    uuid      id                PK
    uuid      tenant_id         FK
    uuid      paquete_id        FK
    string    tipo              "CONSUMO | BONUS | COMPRA | EXCEDENTE | CADUCIDAD"
    int       cantidad          "positivo=ingreso, negativo=consumo"
    int       saldo_resultante
    uuid      documento_id      "null si no aplica"
    uuid      usuario_id
    timestamp ocurrido_en
  }

  CONTADOR_CONSULTAS {
    uuid      id                PK
    uuid      tenant_id         FK
    string    tipo_reporte
    int       total_consultas
    int       periodo_año       "año en curso"
    timestamp ultimo_incremento
  }

  ROL_USUARIO {
    uuid      usuario_id        PK
    uuid      tenant_id         PK
    string    rol               "ADMIN_CUENTA | SUPERVISOR | TECNICO | ASISTENTE"
    timestamp asignado_en
    uuid      asignado_por
  }

  AUDIT_LOG {
    uuid      id                PK
    uuid      tenant_id
    uuid      usuario_id
    string    accion
    string    entidad
    uuid      entidad_id
    jsonb     payload_antes
    jsonb     payload_despues
    string    resultado         "OK | DENEGADO | ERROR"
    string    ip_origen
    timestamp ocurrido_en
  }

  TENANT ||--o{ CONTRATO_ANUAL         : "tiene"
  TENANT ||--o{ PAQUETE_CREDITOS       : "posee"
  TENANT ||--o{ CUOTA_ALMACENAMIENTO   : "genera"
  TENANT ||--o{ CONTADOR_CONSULTAS     : "acumula"
  TENANT ||--o{ ROL_USUARIO            : "define"
  TENANT ||--o{ AUDIT_LOG              : "registra"
  CONTRATO_ANUAL ||--o{ CUOTA_MENSUAL  : "fracciona_en"
  CONTRATO_ANUAL ||--o| RENOVACION     : "genera"
  PAQUETE_CREDITOS ||--o{ EVENTO_CREDITO : "historial"
```

### 4.2 Campos Críticos y Reglas de Integridad

| Tabla | Campo | Tipo | Restricción / Regla |
|-------|-------|------|---------------------|
| `tenant` | `estado` | ENUM | Máquina de estados estricta — solo transiciones permitidas |
| `contrato_anual` | `cuotas_totales` | INT | Siempre 12; no modificable post-creación |
| `contrato_anual` | `cuota_mensual` | DECIMAL | Fija durante los 12 meses; no modificable |
| `cuota_mensual` | `descuento_pct` | DECIMAL | Solo 0, 3 o 10; calculado al momento del pago |
| `renovacion` | `tarifa_renovacion` | DECIMAL | Hardcoded 99.00; configurable solo por migration |
| `paquete_creditos` | `creditos_bonus` | INT | Siempre 4; se inserta automáticamente en el trigger de creación |
| `paquete_creditos` | `saldo_disponible` | INT | `CHECK (saldo_disponible >= 0)` — nunca negativo |
| `evento_credito` | `saldo_resultante` | INT | `CHECK (saldo_resultante >= 0)` — validado antes de insertar |
| `audit_log` | `*` | — | Tabla append-only; sin UPDATE ni DELETE; particionada por mes |

### 4.3 Máquina de Estados del Tenant

```
                    ┌─────────────┐
                    │  ONBOARDING │ ← registro + pago apertura
                    └──────┬──────┘
                           │ apertura confirmada
                           ▼
                    ┌─────────────┐
               ┌──► │   ACTIVE    │ ◄─────────── reactivación pagada
               │    └──────┬──────┘
               │           │ cuota día +2 sin pago / no renovación
               │           ▼
               │    ┌─────────────┐
               │    │  SUSPENDED  │ → genera Cuota de Almacenamiento mensual
               │    └──────┬──────┘
               │           │ 90 días sin reactivar
               │           ▼
               │    ┌─────────────┐
               └────│  CANCELLED  │ → retención datos 90 días → purge
                    └─────────────┘
```

| Transición | Trigger | Condición | Side Effects |
|------------|---------|-----------|--------------|
| `ONBOARDING → ACTIVE` | `payment.apertura.confirmed` | — | Activar accesos; cargar créditos de bienvenida; publicar `tenant.activated` |
| `ACTIVE → SUSPENDED` | Cron diario / evento `cuota.overdue` | Cuota con +2 días sin pago | Bloquear acceso operativo; generar `CuotaAlmacenamiento`; publicar `tenant.suspended` |
| `SUSPENDED → ACTIVE` | Command `ReactivarTenant` | Pago total (cuotas + almacenamiento) | Restaurar acceso; invalidar caché Redis; publicar `tenant.reactivated` |
| `ACTIVE → CANCELLED` | Command `CancelarTenant` | Sin deuda pendiente | Iniciar período retención 90 días; publicar `tenant.cancelled` |
| `SUSPENDED → CANCELLED` | Cron 90 días | Tenant suspendido ≥ 90 días sin pago | Purge programado; publicar `tenant.cancelled` |

### 4.4 Máquina de Estados del Contrato Anual

```
CREATED → ACTIVE → SUSPENDED → ACTIVE (reactivado)
                 ↓                ↓
              EXPIRED ────────► CANCELLED
                 ↓
            RENEWED (nuevo contrato creado)
```

| Transición | Trigger | Regla de negocio |
|------------|---------|-----------------|
| `CREATED → ACTIVE` | Primer pago de cuota exitoso | Acceso al módulo habilitado en < 60 seg |
| `ACTIVE → SUSPENDED` | Cuota impagada día +2 | Contrato NO se cancela; cuotas pendientes siguen exigibles (RF-06.7) |
| `SUSPENDED → ACTIVE` | Pago total de reactivación | Todas las cuotas vencidas + almacenamiento deben estar cubiertas |
| `ACTIVE → EXPIRED` | Fin de período (mes 12 vencido sin renovar) | Acceso desactivado al día siguiente del vencimiento |
| `EXPIRED → RENEWED` | `RenobarContrato` Command | Genera nuevo `ContratoAnual` + cobra tarifa 99 € + primera cuota |
| `* → CANCELLED` | `CancelarContrato` Command | Solo si no hay cuotas vencidas (RF-06.8) |

---

## 5. Domain Events

Todos los eventos se publican al bus de mensajería (Kafka topic: `license-events`). Son inmutables y tienen schema versionado.

### 5.1 Catálogo de Eventos

| Evento | Trigger | Payload clave | Consumidores |
|--------|---------|--------------|--------------|
| `tenant.onboarded` | Apertura de contrato confirmada | `tenantId`, `modalidad`, `creditosBienvenida` | CRM Core, Notifications |
| `tenant.activated` | Primer pago exitoso | `tenantId`, `modulos` | CRM Core, Notifications, Redis invalidation |
| `tenant.suspended` | Cuota impagada día +2 | `tenantId`, `motivoSuspension`, `adeudoTotal` | CRM Core, Notifications, Redis invalidation |
| `tenant.reactivated` | Pago total reactivación | `tenantId`, `montoPagado` | CRM Core, Notifications, Redis invalidation |
| `tenant.cancelled` | Cancelación voluntaria o por mora | `tenantId`, `fechaPurge` | CRM Core, Storage cleanup |
| `contrato.created` | Nuevo contrato anual activado | `contratoId`, `tenantId`, `modulo`, `cuotaMensual` | Billing, Notifications |
| `contrato.renewed` | Renovación anual procesada | `contratoId`, `nuevoContratoId`, `tarifaRenovacion` | Billing, Notifications |
| `contrato.expired` | Contrato vencido sin renovar | `contratoId`, `tenantId`, `modulo` | CRM Core, Redis invalidation |
| `cuota.charged` | Cuota mensual cobrada | `cuotaId`, `monto`, `descuentoPct`, `facturaId` | Billing, Notifications |
| `cuota.overdue` | Cuota llega al día +2 sin pago | `cuotaId`, `contratoId`, `tenantId` | Cron trigger → `SuspenderTenant` |
| `credito.consumed` | Créditos consumidos por PDF (1.0 ó 1.5 según perfil) | `tenantId`, `documentoId`, `usuarioId`, `perfilDocumento`, `costoCreditosAplicado`, `saldoResultante` | Analytics |
| `credito.bonus_granted` | 4 créditos bonus otorgados | `tenantId`, `paqueteId`, `cantidad: 4` | Notifications |
| `credito.balance_low` | Saldo alcanza 20 % / 10 % / 0 % | `tenantId`, `saldoActual`, `umbralAlcanzado` | Notifications |
| `consulta.excedente` | Contador global supera umbral | `tenantId`, `consultaNum`, `creditoCobrado` | Billing |
| `storage.fee_generated` | Cuota de Almacenamiento mensual generada | `tenantId`, `periodo`, `reportes`, `monto` | Billing, Notifications |
| `access.denied` | Acceso denegado por rol o estado | `tenantId`, `userId`, `recurso`, `motivo` | Audit, Security |

### 5.2 Schema Base de Evento (JSON)

```json
{
  "eventId": "uuid-v4",
  "eventType": "tenant.suspended",
  "version": "1.0",
  "tenantId": "uuid",
  "occurredAt": "2026-06-20T14:32:00Z",
  "payload": { ... },
  "metadata": {
    "correlationId": "uuid",
    "causationId": "uuid-del-command-origen",
    "service": "license-service",
    "environment": "prod"
  }
}
```

---

## 6. API Specification

### 6.1 Convenciones Generales

- Base URL: `https://license.mikelcrm.internal/api/v1`
- Autenticación: JWT interno firmado por `auth-service` con claim `tenantId` y `userId`
- Content-Type: `application/json`
- Todos los Commands retornan `202 Accepted` + `correlationId` (procesamiento asíncrono confirmado)
- Todas las Queries retornan `200 OK` con payload o `404` si el recurso no existe

---

### 6.2 Query API (lectura — hot path)

#### `GET /access/{tenantId}`
Validación ultrarrápida de acceso. Consultado por el SaaS Core en cada acción de usuario.

**Respuesta 200:**
```json
{
  "tenantId": "uuid",
  "status": "ACTIVE",
  "modules": ["ADMIN", "REPORTS", "TICKETS"],
  "creditBalance": 42,
  "userRole": "SUPERVISOR",
  "cachedAt": "2026-06-20T14:30:00Z"
}
```

**Respuesta 403 (tenant suspendido):**
```json
{
  "tenantId": "uuid",
  "status": "SUSPENDED",
  "suspendedSince": "2026-06-18",
  "adeudoTotal": 340.50,
  "reactivationUrl": "/api/v1/tenants/{id}/reactivation-summary"
}
```

---

#### `GET /tenants/{tenantId}/contracts`
Estado de todos los contratos anuales del Tenant.

**Respuesta 200:**
```json
{
  "contracts": [
    {
      "contratoId": "uuid",
      "tipo": "MODULO",
      "modulo": "ADMIN",
      "estado": "ACTIVE",
      "cuotaMensual": 200.00,
      "cuotasPagadas": 5,
      "cuotasTotales": 12,
      "proximaFechaCobro": "2026-07-05",
      "fechaVencimientoContrato": "2027-01-05",
      "renovacionAuto": true,
      "descuentoDisponibleHoy": 10,
      "montoConDescuentoHoy": 180.00
    }
  ]
}
```

---

#### `GET /tenants/{tenantId}/credits`
Saldo, historial y contadores de consultas.

**Respuesta 200:**
```json
{
  "saldoDisponible": 42,
  "creditosTotalesAdquiridos": 100,
  "umbralConsultasIncluidas": 10000,
  "contadorGlobalConsultas": 6540,
  "excedente": 0,
  "paqueteActivo": {
    "paqueteId": "uuid",
    "creditosPaquete": 100,
    "creditosBonus": 4,
    "fechaVencimiento": "2027-01-05"
  },
  "alertaNivel": null
}
```

---

#### `GET /tenants/{tenantId}/reactivation-summary`
Desglose del adeudo para pantalla de mora (P-12).

**Respuesta 200:**
```json
{
  "tenantId": "uuid",
  "diasEnMora": 12,
  "cuotasVencidas": [
    { "cuotaId": "uuid", "periodo": "2026-06", "monto": 200.00, "fechaLimite": "2026-06-05" }
  ],
  "cuotasAlmacenamiento": [
    { "periodo": "2026-06", "reportes": 247, "monto": 70.00 },
    { "periodo": "2026-07", "reportes": 252, "monto": 70.00 }
  ],
  "totalCuotasVencidas": 200.00,
  "totalAlmacenamiento": 140.00,
  "totalParaReactivar": 340.00
}
```

---

### 6.3 Command API (escritura)

#### `POST /tenants`
Registrar nuevo Tenant (inicio de onboarding).

**Request:**
```json
{
  "nombre": "Empresa S.A.",
  "emailContacto": "admin@empresa.com",
  "modalidadApertura": "ESTANDAR | PERSONALIZADO"
}
```
**Response 202:** `{ "tenantId": "uuid", "correlationId": "uuid" }`

---

#### `POST /tenants/{tenantId}/contracts`
Crear contrato anual para un módulo.

**Request:**
```json
{
  "tipo": "MODULO",
  "modulo": "ADMIN",
  "cuotaMensual": 200.00,
  "fechaInicio": "2026-07-01",
  "renovacionAuto": true
}
```
**Errores:**
| Código | Caso |
|--------|------|
| 409 | Ya existe contrato ACTIVE para ese módulo en el Tenant (RF-06.4) |
| 422 | Tenant en estado CANCELLED o con adeudo pendiente (RF-06.8) |

---

#### `POST /tenants/{tenantId}/contracts/{contratoId}/cuotas/{cuotaId}/pay`
Registrar pago de una cuota mensual. Aplica descuento según política RF-14.

**Request:**
```json
{
  "fechaPago": "2026-07-03",
  "metodoPago": "STRIPE_TOKEN",
  "referenciaExternal": "pi_xxx"
}
```

**Lógica de descuento (ejecutada en `CuotaPaymentService`):**
```java
public BigDecimal calcularMontoCobrado(Cuota cuota, LocalDate fechaPago) {
  LocalDate fechaLimite = cuota.getFechaLimite();
  if (fechaPago.isBefore(fechaLimite))      return cuota.getMonto().multiply(new BigDecimal("0.90")); // 10% dto
  if (fechaPago.isEqual(fechaLimite))       return cuota.getMonto().multiply(new BigDecimal("0.97")); // 3% dto
  if (fechaPago.isEqual(fechaLimite.plusDays(1))) return cuota.getMonto();                              // precio normal
  throw new CuotaVencidaException("Cuota en mora — usar endpoint de reactivación");
}
```

---

#### `POST /tenants/{tenantId}/contracts/{contratoId}/renew`
Procesar renovación anual. Cobra tarifa 99 € + primera cuota del nuevo período.

**Request:** `{ "metodoPago": "STRIPE_TOKEN" }`

**Lógica:**
1. Validar que el contrato esté en Ventana de Renovación (días -30 a 0) o en EXPIRED
2. Cobrar tarifa de renovación 99 €
3. Crear nuevo `ContratoAnual` (copia del anterior con fechas +12 meses)
4. Cobrar primera cuota del nuevo período (con descuento si aplica)
5. Publicar `contrato.renewed`

---

#### `POST /tenants/{tenantId}/credits/packages`
Adquirir paquete de créditos anuales. Otorga 4 créditos bonus automáticamente.

**Request:**
```json
{ "cantidadCreditos": 100, "metodoPago": "STRIPE_TOKEN" }
```

**Lógica:**
```java
// En CreditCommandService.adquirirPaquete()
PaqueteCreditos paquete = new PaqueteCreditos();
paquete.setCreditosPaquete(request.getCantidadCreditos());
paquete.setCreditosBonus(4);  // RN-C11: siempre 4, sin excepción
paquete.setSaldoDisponible(request.getCantidadCreditos() + 4);
paquete.setFechaVencimiento(LocalDate.now().plusYears(1));
// Publicar credito.bonus_granted
```

---

#### `POST /tenants/{tenantId}/credits/consume`
Consumir créditos al generar un PDF. El costo depende del `perfilDocumento` (RF-07.1, RF-07.17). Implementa reserva atómica (RF-07.2, RN-C-PDF-03, RN-C-PDF-04).

**Perfiles disponibles:**

| `perfilDocumento` | Costo | Denominación anterior |
|-------------------|-------|-----------------------|
| `ASISTENCIA_DIGITAL` | 1.0 crédito | — |
| `ASISTENTE_AUTOMATICO_NORMAS` | 1.5 créditos | ~~Asistente de Cumplimiento de Normas~~ |

**Request:**
```json
{
  "documentoId": "uuid",
  "usuarioId": "uuid",
  "perfilDocumento": "ASISTENCIA_DIGITAL"
}
```

**Lógica de reserva atómica:**
```java
// Costo por perfil (RF-07.17) — configurable en tabla perfil_documento_config
private static final Map<PerfilDocumento, BigDecimal> COSTO_CREDITOS = Map.of(
  PerfilDocumento.ASISTENCIA_DIGITAL,         new BigDecimal("1.0"),
  PerfilDocumento.ASISTENTE_AUTOMATICO_NORMAS, new BigDecimal("1.5")
);

// Ejecutado en transacción con SELECT FOR UPDATE
@Transactional(isolation = SERIALIZABLE)
public void consumirCredito(UUID tenantId, ConsumoRequest req) {
  BigDecimal costo = COSTO_CREDITOS.get(req.getPerfilDocumento());
  PaqueteCreditos paquete = repo.findActiveByTenantForUpdate(tenantId);
  if (paquete.getSaldoDisponible().compareTo(costo) < 0)
    throw new SaldoInsuficienteException(paquete.getSaldoDisponible(), costo); // HTTP 402
  BigDecimal saldoAntes = paquete.getSaldoDisponible();
  paquete.setSaldoDisponible(saldoAntes.subtract(costo));
  eventoRepo.save(new EventoCredito(
    tenantId, CONSUMO,
    costo.negate(),          // RF-07.19: costo registrado
    paquete.getSaldoDisponible(),
    req.getPerfilDocumento(),
    req.getDocumentoId(),
    req.getUsuarioId()
  ));
  // Si falla la generación del PDF → compensación: +costo créditos en ≤ 30 seg (RF-07.2, RN-C-PDF-04)
}
```

---

#### `POST /tenants/{tenantId}/reactivate`
Reactivar Tenant suspendido. Requiere pago total (cuotas vencidas + almacenamiento).

**Request:**
```json
{ "metodoPago": "STRIPE_TOKEN", "montoEsperado": 340.00 }
```

**Validación:**
```java
BigDecimal totalAdeudo = calcularTotalCuotasVencidas(tenantId)
    .add(calcularTotalAlmacenamientoAcumulado(tenantId));
if (request.getMontoEsperado().compareTo(totalAdeudo) != 0)
    throw new MontoIncorrectoException(totalAdeudo); // RF-14.9: pago total obligatorio
```

---

#### `DELETE /tenants/{tenantId}/contracts/{contratoId}`
Solicitar downgrade (no renovar módulo al vencimiento del contrato anual).

**Validaciones:**
- El contrato debe estar ACTIVE (RF-06.2)
- No puede haber cuotas vencidas pendientes (RF-06.8)
- Efectivo: el contrato no se renovará al vencimiento; acceso activo hasta fecha fin

---

### 6.4 Consultas Incrementales — Contadores de Consultas

#### `POST /tenants/{tenantId}/consultations`
Incrementar contador de consultas (+1) por cada visualización, descarga, firma o reenvío.

**Request:** `{ "tipoReporte": "INFORME_TECNICO", "documentoId": "uuid", "usuarioId": "uuid" }`

**Lógica de excedente (RF-08.5–08.7):**
```java
// Ejecutado asíncronamente para no bloquear el SaaS Core
void procesarConsulta(UUID tenantId, String tipoReporte) {
  // Incrementar contador por tipo y contador global
  int globalActual = contadorRepo.incrementGlobal(tenantId);
  contadorRepo.incrementPorTipo(tenantId, tipoReporte);

  int umbral = creditoRepo.getUmbralConsultas(tenantId); // creditos_totales * 100
  int excedente = globalActual - umbral;

  if (excedente > 0 && excedente % 100 == 0) {
    // Cada 100 consultas de excedente → cobrar 1 crédito (RF-08.6)
    creditCommandService.cobrarExcedenteConsultas(tenantId);
    eventoPublisher.publish(new ConsultaExcedenteEvent(tenantId, globalActual));
  }
}
```

---

## 7. Reglas de Negocio (Consolidadas)

> Fuente de verdad para toda la lógica del `license-service`. Cada regla tiene ID único, owner de implementación y test asociado.

### 7.1 Contratos Anuales

| ID | Regla | Test |
|----|-------|------|
| RN-CONT-01 | Todos los contratos son anuales con 12 cuotas mensuales. No existe modalidad mensual renovable | TC-CONT-01 |
| RN-CONT-02 | La cuota mensual es fija para los 12 meses del contrato; no puede modificarse una vez creado | TC-CONT-02 |
| RN-CONT-03 | El upgrade (agregar módulo) genera un nuevo contrato independiente con su propio ciclo de 12 cuotas; no modifica contratos existentes | TC-CONT-03 |
| RN-CONT-04 | El downgrade solo puede solicitarse mientras el contrato está ACTIVE; es efectivo al vencer el año; no genera reembolso | TC-CONT-04 |
| RN-CONT-05 | Una empresa no puede tener dos contratos ACTIVE para el mismo módulo simultáneamente | TC-CONT-05 |
| RN-CONT-06 | No se puede solicitar baja de un contrato si hay cuotas vencidas pendientes | TC-CONT-06 |
| RN-CONT-07 | La suspensión por cuota impagada no extingue el contrato; las cuotas pendientes siguen siendo exigibles | TC-CONT-07 |

### 7.2 Cuotas Mensuales y Descuentos

| ID | Regla | Test |
|----|-------|------|
| RN-PAG-01 | Pago antes de la Fecha Límite (cualquier día anterior) → descuento del 10 % sobre el monto de la cuota | TC-PAG-01 |
| RN-PAG-02 | Pago el mismo día de la Fecha Límite → descuento del 3 % | TC-PAG-02 |
| RN-PAG-03 | Pago el día +1 (día de gracia) → precio normal sin descuento ni penalización | TC-PAG-03 |
| RN-PAG-04 | Día +2 sin pago → suspensión del Tenant + generación de Cuota de Almacenamiento | TC-PAG-04 |
| RN-PAG-05 | El descuento se refleja en la factura emitida; el monto facturado es el monto descontado | TC-PAG-05 |
| RN-PAG-06 | Los descuentos no son acumulables con ningún otro descuento o promoción | TC-PAG-06 |

### 7.3 Renovación Anual

| ID | Regla | Test |
|----|-------|------|
| RN-REN-01 | La Ventana de Renovación se abre en el día -30 antes de la Fecha de Vencimiento del Contrato | TC-REN-01 |
| RN-REN-02 | El sistema envía notificaciones en los días -30, -7 y -1, cada una con descuento disponible y tarifa de renovación (99 €) | TC-REN-02 |
| RN-REN-03 | La renovación automática cobra: tarifa fija de 99 € + primera cuota mensual del nuevo período | TC-REN-03 |
| RN-REN-04 | Si el Cliente no desactiva la renovación automática antes del día -1, el cobro se ejecuta en la Fecha de Vencimiento | TC-REN-04 |
| RN-REN-05 | La tarifa de 99 € aplica por cada contrato renovado; no se consolida si varios contratos vencen el mismo día | TC-REN-05 |
| RN-REN-06 | Si el cobro de renovación falla, el Tenant queda SUSPENDED al día siguiente (día +1 post-vencimiento) | TC-REN-06 |

### 7.4 Suspensión y Cuota de Almacenamiento

| ID | Regla | Fórmula / Detalle | Test |
|----|-------|-------------------|------|
| RN-SUS-01 | Al activarse la suspensión, el acceso operativo se bloquea; la consulta en modo solo lectura sigue disponible | — | TC-SUS-01 |
| RN-SUS-02 | La Cuota de Almacenamiento se calcula sobre los reportes almacenados **en el momento de generarla** | `50 + CEIL(MAX(reportes-100,0)/100) × 10` | TC-SUS-02 |
| RN-SUS-03 | Ejemplos de la fórmula: 0–100 rep = 50 €; 101–200 = 60 €; 201–300 = 70 €; 301–400 = 80 € | — | TC-SUS-03 |
| RN-SUS-04 | La Cuota de Almacenamiento se genera el primer día de cada mes de suspensión activa | — | TC-SUS-04 |
| RN-SUS-05 | Las Cuotas de Almacenamiento acumuladas no caducan mientras la cuenta esté suspendida o en período de retención | — | TC-SUS-05 |
| RN-SUS-06 | Para reactivar, el pago debe cubrir 100 % del adeudo: cuotas vencidas + almacenamiento acumulado. No se acepta pago parcial | — | TC-SUS-06 |
| RN-SUS-07 | El acceso se restaura en menos de 60 segundos tras confirmar el pago total de reactivación | — | TC-SUS-07 |

### 7.5 Créditos

| ID | Regla | Test |
|----|-------|------|
| RN-CRED-01 | El costo en créditos al generar un PDF depende del Perfil de Documento: `ASISTENCIA_DIGITAL = 1.0`, `ASISTENTE_AUTOMATICO_NORMAS = 1.5`. Se descuenta al confirmar la generación exitosa del PDF | TC-CRED-01 |
| RN-CRED-02 | El costo del perfil se **reserva** antes de iniciar la generación; si el PDF falla, el importe exacto se reintegra en ≤ 30 seg. No existe descuento parcial | TC-CRED-02 |
| RN-CRED-03 | Firmas, descargas, visualizaciones y reenvíos NO consumen crédito pero SÍ incrementan el contador de consultas | TC-CRED-03 |
| RN-CRED-04 | Todo paquete anual de créditos entrega automáticamente **4 créditos bonus** sin excepción | TC-CRED-04 |
| RN-CRED-05 | Los créditos bonus se fusionan con el saldo general; su vencimiento es igual al del paquete que los origina | TC-CRED-05 |
| RN-CRED-06 | Los créditos no consumidos al vencimiento del período anual caducan y no se transfieren | TC-CRED-06 |
| RN-CRED-07 | El sistema bloquea el envío de forma si `saldo_disponible < costo_perfil`, antes de intentar generar el PDF. El error incluye saldo actual y costo requerido | TC-CRED-07 |
| RN-CRED-08 | El umbral de consultas incluidas = `créditos_totales_adquiridos × 100` | TC-CRED-08 |
| RN-CRED-09 | Cada bloque completo de 100 consultas de excedente cobra 1 crédito; las fracciones no se cobran hasta completar el bloque | TC-CRED-09 |
| RN-CRED-10 | Si el saldo es 0 al generarse un cobro por excedente, se registra la deuda pero no se bloquean las consultas | TC-CRED-10 |
| RN-CRED-11 | El saldo de créditos se persiste como `NUMERIC(10,2)` en PostgreSQL y como `BigDecimal` en Java. Nunca como `float` ni `double` para evitar errores de precisión en operaciones con 1.5 créditos | TC-CRED-11 |
| RN-CRED-12 | `ASISTENTE_AUTOMATICO_NORMAS` es la denominación técnica canónica del perfil. La denominación anterior "Asistente de Cumplimiento de Normas" es obsoleta y no debe usarse en código, APIs ni documentación | — |

### 7.6 Apertura de Contrato (Onboarding)

| ID | Regla | Test |
|----|-------|------|
| RN-OPEN-01 | Todo Tenant nuevo debe pagar la Apertura de Contrato antes de acceder al CRM | TC-OPEN-01 |
| RN-OPEN-02 | Apertura Estándar: 400 €, 2 créditos de bienvenida, reporte sin marca | TC-OPEN-02 |
| RN-OPEN-03 | Apertura Personalizada: 1.400 €, 10 créditos de bienvenida, reporte con marca del Cliente | TC-OPEN-03 |
| RN-OPEN-04 | Los créditos de bienvenida no caducan antes de los primeros 12 meses de contrato | TC-OPEN-04 |
| RN-OPEN-05 | Upgrade de Estándar a Personalizada: 1.000 € de diferencia + 8 créditos adicionales | TC-OPEN-05 |

### 7.7 RBAC — Roles y Permisos

| Rol | Permisos | Restricciones |
|-----|----------|---------------|
| `ADMIN_CUENTA` | Gestión de contratos, usuarios, licencias, créditos | Solo sobre su propio Tenant |
| `SUPERVISOR` | Tablero, tickets, aprobación, eFirma, historial | Solo módulo ADMIN activo |
| `TECNICO` (Técnico de campo) | Enviar formas, generar PDF con perfil habilitado (1.0 ó 1.5 créditos según perfil), ver reportes propios | Consume créditos del Tenant según Perfil de Documento |
| `ASISTENTE` | Leer/crear/actualizar clientes y tickets; consultar reportes | Sin acceso a aprobación, config ni créditos |

---

## 8. Jobs Programados (Cron)

| Job | Cron Expression | Función | Lógica crítica |
|-----|----------------|---------|---------------|
| `CuotaOverdueJob` | `0 0 1 * * *` (1 AM diario) | Detectar cuotas con Fecha Límite +2 sin pago → publicar `cuota.overdue` | Carga cuotas `EN_MORA`; por cada una dispara `SuspenderTenantCommand` |
| `RenovacionNotificacionJob` | `0 0 8 * * *` (8 AM diario) | Enviar alertas de renovación en días -30, -7, -1 | Carga contratos donde `fecha_vencimiento - hoy ∈ {30, 7, 1}` |
| `RenovacionAutoJob` | `0 0 9 * * *` (9 AM diario) | Ejecutar renovación automática en contratos que vencen hoy | Solo contratos con `renovacion_auto = true`; si cobro falla → publica `cuota.overdue` |
| `CuotaAlmacenamientoJob` | `0 0 2 1 * *` (día 1 de cada mes, 2 AM) | Generar Cuota de Almacenamiento para Tenants SUSPENDED | Calcula `reportes_almacenados` actual; aplica fórmula; inserta `CuotaAlmacenamiento` |
| `CreditosVencimientoJob` | `0 0 3 * * *` (3 AM diario) | Caducar créditos de paquetes con `fecha_vencimiento = ayer` | Pone saldo a 0; publica `credito.expired`; notifica |
| `TenantCancelacionJob` | `0 0 4 * * *` (4 AM diario) | Cancelar Tenants SUSPENDED ≥ 90 días sin pago | Verifica `fecha_suspension + 90 days <= hoy`; publica `tenant.cancelled` |

---

## 9. Seguridad

| Área | Requisito | Implementación |
|------|-----------|----------------|
| Autenticación | JWT firmado con RS256 por `auth-service`; TTL 15 min | Spring Security + `JwtAuthFilter` |
| Autorización inter-servicios | Service-to-service con JWT de servicio (`iss: saas-core`) | Claim `service` validado en filtro |
| Confinamiento de Tenant | Todo query y command incluye `tenantId` del JWT; imposible acceder a datos de otro Tenant | `@PreAuthorize("@tenantGuard.check(#tenantId, authentication)")` |
| Audit Log | Toda acción registrada en tabla append-only con `ip_origen` y actor | AOP `@AuditAction` en todos los Commands |
| Datos sensibles de pago | Nunca almacenados; solo `referenciaExternal` del Payment Service | Sin campos de tarjeta en ninguna tabla |
| Rate limiting | Query API: 1000 req/s por Tenant; Command API: 100 req/s por Tenant | Redis sliding window counter |

---

## 10. Rendimiento y Escalabilidad

| Escenario | Volumen esperado | SLA | Estrategia |
|-----------|-----------------|-----|------------|
| Validación de acceso (Query hot path) | 500 req/s pico por instancia | < 5 ms p99 | Redis hit; escala horizontal stateless |
| Cache miss (cold start o invalidación) | < 5 % de las requests | < 30 ms p99 | PostgreSQL read replica indexada |
| Command de pago de cuota | 100 req/s pico | < 200 ms p99 | PostgreSQL primary con connection pool (HikariCP) |
| Cron `CuotaOverdueJob` | Hasta 10.000 contratos/día | < 5 min total | Batch de 500 con `@Async` + backoff |
| Cron `RenovacionAutoJob` | Hasta 2.000 renovaciones/día | < 10 min total | Queue Kafka + consumo paralelo con 4 workers |
| Consultas de excedente (incremento contador) | Async, no bloquea | Eventual (≤ 5 seg) | Kafka consumer group; Redis sorted set para contadores |

### 10.1 Índices Críticos PostgreSQL

```sql
-- Hot path: validación de acceso
CREATE INDEX CONCURRENTLY idx_contrato_tenant_estado
  ON contrato_anual (tenant_id, estado)
  WHERE estado IN ('ACTIVE', 'SUSPENDED');

-- Cron jobs: cuotas vencidas
CREATE INDEX CONCURRENTLY idx_cuota_fecha_estado
  ON cuota_mensual (fecha_limite, estado)
  WHERE estado = 'PENDIENTE';

-- Cron jobs: renovaciones próximas
CREATE INDEX CONCURRENTLY idx_contrato_vencimiento_auto
  ON contrato_anual (fecha_vencimiento, renovacion_auto)
  WHERE estado = 'ACTIVE' AND renovacion_auto = true;

-- Contadores de consultas
CREATE INDEX CONCURRENTLY idx_contador_tenant_periodo
  ON contador_consultas (tenant_id, periodo_año);

-- Audit log (particionado por mes)
CREATE TABLE audit_log_2026_06 PARTITION OF audit_log
  FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
```

---

## 11. Plan de Pruebas

### 11.1 Casos de Prueba Críticos

| ID | Escenario | Resultado Esperado |
|----|-----------|-------------------|
| TC-CONT-01 | Intentar crear contrato mensual renovable | HTTP 422 — solo contratos anuales |
| TC-PAG-01 | Pagar cuota 3 días antes de fecha límite | Descuento 10 % aplicado; factura por 180 € (cuota 200 €) |
| TC-PAG-04 | Cuota no pagada llega al día +2 | `CuotaOverdueJob` dispara `SuspenderTenantCommand`; Tenant SUSPENDED; Cuota Almacenamiento generada |
| TC-SUS-02 | Tenant con 247 reportes → Cuota Almacenamiento | `50 + CEIL((247-100)/100)*10 = 50 + 20 = 70 €` |
| TC-SUS-06 | Intento de reactivación con pago parcial | HTTP 422 — `MontoIncorrectoException` con total correcto |
| TC-REN-03 | Renovación automática en fecha de vencimiento | Cobro 99 € (tarifa) + 200 € (primera cuota nuevo período, con descuento si se paga el mismo día = 3 %) |
| TC-CRED-04 | Comprar paquete de 50 créditos | Saldo resultante = 54 (50 + 4 bonus); `credito.bonus_granted` publicado |
| TC-CRED-02 | PDF falla después de reservar crédito | Crédito reintegrado en ≤ 30 segundos; saldo sin cambio |
| TC-CRED-09 | Consultas: 10.001 sobre umbral de 10.000 | 1 crédito cobrado en consulta 10.001; deuda registrada si saldo = 0 |
| TC-SUS-07 | Pago total de reactivación | Acceso restaurado en < 60 segundos; Redis invalidado; `tenant.reactivated` publicado |

### 11.2 Estrategia de Testing

| Tipo | Cobertura | Herramienta |
|------|-----------|-------------|
| Unit tests | 100 % de reglas RN-* | JUnit 5 + Mockito |
| Integration tests | Todos los endpoints Command y Query | Spring Boot Test + Testcontainers (PostgreSQL + Redis) |
| Contract tests | Eventos publicados en Kafka | Spring Cloud Contract |
| Load tests | Escenarios de pico (sección 10) | k6 |
| Security tests | Intentos cross-tenant, privilege escalation | OWASP ZAP + tests manuales |

---

## 12. Plan de Implementación

| Fase | Entregable | Días | Dependencias |
|------|-----------|------|--------------|
| 1 — Infraestructura | Setup Spring Boot + PostgreSQL + Redis + Kafka local | 2 | — |
| 2 — Modelo de datos | Migraciones Flyway: todas las tablas + índices | 2 | Fase 1 |
| 3 — Query API | `AccessQueryService` + Redis cache + hot path | 3 | Fase 2 |
| 4 — Command API core | Contratos, cuotas, pagos, descuentos | 5 | Fase 2 |
| 5 — Créditos | Consumo, bonus, contadores, excedente | 3 | Fase 4 |
| 6 — Cron Jobs | Los 6 jobs programados | 3 | Fase 4, 5 |
| 7 — Eventos | Publicación Kafka + event handlers externos | 2 | Fase 4 |
| 8 — RBAC | Roles, permisos, middleware RBAC | 2 | Fase 3 |
| 9 — Audit Log | AOP `@AuditAction` + tabla particionada | 1 | Fase 4 |
| 10 — QA & Load test | Todos los TC-* + k6 + security tests | 4 | Fases 1-9 |

**Total estimado:** ~27 días de desarrollo.

---

## 13. Preguntas Abiertas

| # | Pregunta | Owner | Estado |
|---|----------|-------|--------|
| Q-01 | ¿Penalización por baja anticipada antes de completar los 12 meses? (RN-ADM-05) | Comercial | Abierta |
| Q-02 | ¿El Módulo Administrador incluye créditos de uso o solo acceso funcional? (RN-ADM-06) | Producto | Abierta |
| Q-03 | ¿Precio y modalidad del Módulo Admin tras renovar los 12 meses? (RN-ADM-07) | Comercial | Abierta |
| Q-04 | ¿Bus de mensajería: Kafka o RabbitMQ? Impacta configuración de `RenovacionAutoJob` | Arquitectura | Abierta |
| Q-05 | ¿Read replica de PostgreSQL desde el inicio o cuando se alcance cierto volumen de Tenants? | Infra | Abierta |
| Q-06 | ¿TTL del caché Redis configurable por Tenant VIP o fijo en 300 seg para todos? | Arquitectura | Abierta |

---

## 14. Glosario Técnico

| Término | Definición |
|---------|-----------|
| CQRS | Command Query Responsibility Segregation — patrón que separa operaciones de escritura (Commands) de lectura (Queries) para optimizar cada camino independientemente |
| Hot path | Camino de código ejecutado en cada acción del usuario; debe ser ultrarrápido (< 5 ms). En este servicio: `GET /access/{tenantId}` |
| Cache invalidation | Proceso de eliminar o actualizar la clave Redis de un Tenant cuando su estado cambia, forzando un reload desde PostgreSQL en la siguiente consulta |
| Domain Event | Hecho inmutable del dominio publicado al bus de mensajería; otros servicios reaccionan a él de forma asíncrona |
| Compensación | Operación de reversión cuando un proceso falla a mitad de camino (ej: reintegrar crédito si el PDF falla) |
| Tenant | Organización cliente que opera en una instancia lógicamente aislada del Mikel CRM; unidad base de toda la gestión de licencias |
| Ventana de Renovación | Período de 30 días previos al vencimiento anual durante el cual el sistema notifica y procesa la renovación |

---

## 15. Anexos

- [ ] Diagrama de secuencia — hot path de validación de acceso (Redis hit vs miss)
- [ ] Diagrama de secuencia — flujo de suspensión por cuota vencida (día +2)
- [ ] Diagrama de secuencia — renovación automática anual
- [ ] Configuración Flyway — orden de migraciones
- [ ] Configuración HikariCP + Redis connection pool para producción
- [ ] ADR-001: CQRS vs monolito — justificación de la decisión

---

> **Checklist de aprobación:**
> - [ ] Arquitecto de Software revisó sección 3 (arquitectura CQRS) y sección 10 (rendimiento)
> - [ ] Product Owner validó todas las reglas de negocio sección 7 (RN-*)
> - [ ] Tech Lead revisó modelo de datos sección 4 e índices PostgreSQL
> - [ ] QA Lead revisó plan de pruebas sección 11
> - [ ] Security revisó sección 9
> - [ ] Decisiones comerciales abiertas (Q-01, Q-02, Q-03) resueltas antes de implementar Fase 4
