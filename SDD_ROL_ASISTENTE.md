# SDD — Rol Asistente (RBAC)
**Proyecto:** `Mikel CRM — Sistema de Licencias`  
**Feature / Módulo:** `Control de Acceso por Rol — Asistente`  
**Versión:** `0.1 — Draft`  
**Autor(es):** `Analista de Sistemas — LicenciasCRMMikel`  
**Revisores:** `Tech Lead, Product Owner, QA Lead`  
**Fecha:** `2026-06-19`  
**Estado:** `Draft`

---

## 0. Change Log

| Versión | Fecha | Autor | Cambio |
|---------|-------|-------|--------|
| 0.1 | 2026-06-19 | Analista de Sistemas | Draft inicial — definición del rol Asistente |

---

## 1. Resumen Ejecutivo

**Problema:** El sistema actual define actores de alto nivel (Cliente/Empresa, Admin CRM, Supervisor) pero carece de un rol operativo de nivel medio que permita a personal de soporte gestionar tickets y clientes sin acceso a funciones administrativas o de aprobación.

**Solución propuesta:** Definir e implementar el rol **Asistente** dentro del sistema RBAC del Mikel CRM. Este rol tiene acceso de lectura/gestión sobre clientes y tickets, consulta de reportes existentes, y está explícitamente bloqueado de la configuración del sistema y la aprobación de documentos.

**Impacto esperado:** Reducción del riesgo de acceso indebido a configuraciones críticas; habilitación de operadores de soporte para trabajar de forma autónoma sin requerir permisos de Supervisor o Admin CRM.

---

## 2. Objetivos y Alcance

### 2.1 En Scope

- Definición del rol `ASISTENTE` en el modelo RBAC del sistema
- Permisos sobre el módulo de Clientes y Tickets (lectura + gestión)
- Permisos de consulta sobre Reportes existentes (solo lectura)
- Bloqueo explícito sobre Configuración del Sistema
- Bloqueo explícito sobre Aprobación de Documentos y eFirma
- User stories específicas del rol Asistente
- Casos de prueba de autorización (happy path + acceso denegado)

### 2.2 Fuera de Scope (Non-Goals)

- Gestión de licencias o suscripciones (exclusivo del rol Admin de cuenta)
- Generación de nuevos reportes o plantillas (solo consulta)
- Aprobación o firma electrónica de documentos
- Configuración de módulos, integraciones o parámetros del sistema
- Asignación o gestión de otros usuarios/roles dentro del tenant
- Consumo de créditos directamente (el rol Asistente no envía formas técnicas — eso es rol Técnico)

### 2.3 Criterios de Éxito

| Métrica | Baseline | Target | Cómo se mide |
|---------|----------|--------|--------------|
| Tiempo de resolución de autorización | N/A | < 50 ms p99 | APM / logs de middleware |
| Intentos de acceso no autorizado bloqueados | N/A | 100 % | Log de auditoría |
| Cobertura de tests de autorización del rol | 0 % | 100 % de reglas RN-AST-* | Suite de tests |

---

## 3. Contexto y Antecedentes

### 3.1 Estado Actual del Sistema

El sistema define tres actores principales en `REQUERIMIENTOS_HISTORIAS_USUARIO.md`:

| Actor existente | Nivel de acceso |
|----------------|-----------------|
| Cliente / Empresa | Autogestión de su cuenta y licencias |
| Admin CRM (interno) | Gestión total sobre cualquier licencia |
| Sistema (automatizado) | Procesos internos de cobro y notificación |
| Supervisor / Coordinador | Módulo Administrador: tablero, tickets, aprobación, eFirma |

El rol **Técnico** aparece implícitamente en RF-07 y US-18 como el actor que envía formas y consume créditos. Sin embargo, no existe un rol formal de **Asistente** que cubra la operativa de soporte al cliente (tickets de servicio + captura de datos de cliente) sin acceso a aprobación ni configuración.

### 3.2 Decisiones Previas Relevantes

- `RF-01.5` — El sistema restringe acceso a módulos sin licencia activa; el mismo principio aplica a nivel de rol para funcionalidades individuales.
- `US-04` — Definida la pantalla de bloqueo cuando un usuario sin permiso intenta acceder a un módulo; aplica también a nivel de funcionalidad dentro de un módulo.
- `US-34` — El ciclo de vida de tickets (`Solicitado → Cerrado`) es la base sobre la que el Asistente opera.
- `US-35` — La aprobación con eFirma de Supervisor está reservada a rol Supervisor; el Asistente no puede ejecutar este paso.

### 3.3 Restricciones

| Tipo | Descripción |
|------|-------------|
| Técnica | El middleware de autorización debe evaluar permisos en cada request; no puede depender de estado en cliente |
| Negocio | El Asistente opera dentro de un tenant; sus permisos son a nivel tenant, no cross-tenant |
| Seguridad | Las acciones del Asistente deben registrarse en el audit log inmutable con su identidad |
| Funcional | El rol Asistente no puede escalar sus propios privilegios ni gestionar otros usuarios |

---

## 4. Modelo de Datos

### 4.1 Entidades Principales

```
erDiagram
    ROL {
        uuid id PK
        string codigo
        string nombre
        string descripcion
        bool activo
    }

    PERMISO {
        uuid id PK
        string recurso
        string accion
        string descripcion
    }

    ROL_PERMISO {
        uuid rol_id FK
        uuid permiso_id FK
        bool concedido
    }

    USUARIO_ROL {
        uuid usuario_id FK
        uuid rol_id FK
        uuid tenant_id FK
        date fecha_asignacion
        uuid asignado_por FK
    }

    USUARIO {
        uuid id PK
        string email
        uuid tenant_id FK
    }

    ROL ||--o{ ROL_PERMISO : tiene
    PERMISO ||--o{ ROL_PERMISO : aplicada_en
    USUARIO ||--o{ USUARIO_ROL : tiene
    ROL ||--o{ USUARIO_ROL : asignado_a
```

### 4.2 Definición de Campos Críticos

| Entidad | Campo | Tipo | Nullable | Descripción | Reglas de negocio |
|---------|-------|------|----------|-------------|-------------------|
| Rol | `codigo` | enum | No | Identificador técnico del rol | `ADMIN_CRM`, `SUPERVISOR`, `TECNICO`, `ASISTENTE`, `CLIENTE` |
| Permiso | `recurso` | string | No | Módulo o entidad objetivo | `clientes`, `tickets`, `reportes`, `config_sistema`, `aprobacion_docs` |
| Permiso | `accion` | string | No | Operación permitida | `leer`, `crear`, `actualizar`, `eliminar`, `aprobar`, `firmar`, `configurar` |
| RolPermiso | `concedido` | bool | No | Permite deny explícito además de allow | Default `true`; un `false` bloquea aunque otro rol conceda |
| UsuarioRol | `tenant_id` | uuid | No | Alcance del rol al tenant del usuario | Un usuario solo puede operar dentro de su tenant |

### 4.3 Matriz de Permisos del Rol Asistente

| Recurso | Leer | Crear | Actualizar | Eliminar | Aprobar | Firmar | Configurar |
|---------|------|-------|------------|----------|---------|--------|------------|
| `clientes` | ✅ | ✅ | ✅ | ❌ | — | — | — |
| `tickets` | ✅ | ✅ | ✅ | ❌ | — | — | — |
| `reportes` | ✅ | ❌ | ❌ | ❌ | — | — | — |
| `config_sistema` | ❌ | ❌ | ❌ | ❌ | — | — | ❌ |
| `aprobacion_docs` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | — |
| `creditos` | ❌ | ❌ | ❌ | ❌ | — | — | — |
| `licencias` | ❌ | ❌ | ❌ | ❌ | — | — | — |

> **Leyenda:** ✅ Permitido · ❌ Denegado · — No aplica para este recurso/acción

### 4.4 Seed de Permisos del Rol Asistente

```sql
-- Seed: permisos del rol ASISTENTE
INSERT INTO rol_permiso (rol_id, permiso_id, concedido)
SELECT r.id, p.id, true
FROM roles r, permisos p
WHERE r.codigo = 'ASISTENTE'
  AND (
      (p.recurso = 'clientes'  AND p.accion IN ('leer', 'crear', 'actualizar'))
   OR (p.recurso = 'tickets'   AND p.accion IN ('leer', 'crear', 'actualizar'))
   OR (p.recurso = 'reportes'  AND p.accion = 'leer')
  );

-- Deny explícito (defensivo)
INSERT INTO rol_permiso (rol_id, permiso_id, concedido)
SELECT r.id, p.id, false
FROM roles r, permisos p
WHERE r.codigo = 'ASISTENTE'
  AND (
      p.recurso = 'config_sistema'
   OR p.recurso = 'aprobacion_docs'
   OR p.recurso = 'creditos'
   OR p.recurso = 'licencias'
  );
```

---

## 5. Especificación de API

### 5.1 Convención de Autorización

Todos los endpoints validan el rol del token JWT antes de procesar la request:

```
Authorization: Bearer <JWT>
Claims: { sub: usuario_id, tenant_id, rol: "ASISTENTE", ... }
```

El middleware de autorización aplica la evaluación: `¿tiene el rol el permiso (recurso, acción)?`

### 5.2 Endpoints Accesibles por el Rol Asistente

#### Módulo Clientes

| Método | Endpoint | Permiso requerido | Asistente |
|--------|----------|-------------------|-----------|
| GET | `/api/v1/clientes` | `clientes:leer` | ✅ |
| GET | `/api/v1/clientes/{id}` | `clientes:leer` | ✅ |
| POST | `/api/v1/clientes` | `clientes:crear` | ✅ |
| PATCH | `/api/v1/clientes/{id}` | `clientes:actualizar` | ✅ |
| DELETE | `/api/v1/clientes/{id}` | `clientes:eliminar` | ❌ → 403 |

**POST /api/v1/clientes — Request Body:**
```json
{
  "nombre": "string",
  "email": "string",
  "telefono": "string",
  "empresa": "string",
  "notas": "string"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "nombre": "string",
  "tenant_id": "uuid",
  "creado_por": "uuid",
  "created_at": "ISO8601"
}
```

**Errores comunes:**

| Código | Caso |
|--------|------|
| 400 | Campos requeridos faltantes (nombre, email) |
| 403 | Acción no permitida para el rol Asistente |
| 409 | Email de cliente ya registrado en el tenant |

---

#### Módulo Tickets

| Método | Endpoint | Permiso requerido | Asistente |
|--------|----------|-------------------|-----------|
| GET | `/api/v1/tickets` | `tickets:leer` | ✅ |
| GET | `/api/v1/tickets/{id}` | `tickets:leer` | ✅ |
| POST | `/api/v1/tickets` | `tickets:crear` | ✅ |
| PATCH | `/api/v1/tickets/{id}` | `tickets:actualizar` | ✅ (con restricciones — ver RN-AST-04) |
| PATCH | `/api/v1/tickets/{id}/estado` | `tickets:actualizar` | ✅ (estados permitidos — ver RN-AST-05) |
| DELETE | `/api/v1/tickets/{id}` | `tickets:eliminar` | ❌ → 403 |

**POST /api/v1/tickets — Request Body:**
```json
{
  "cliente_id": "uuid",
  "tipo_servicio": "string",
  "descripcion": "string",
  "prioridad": "baja | media | alta | urgente",
  "fecha_limite": "ISO8601 | null"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "estado": "solicitado",
  "cliente_id": "uuid",
  "creado_por": "uuid",
  "tenant_id": "uuid",
  "created_at": "ISO8601"
}
```

> **Nota:** El Asistente crea tickets en estado `solicitado`. La asignación a un técnico y el avance del ticket a estados posteriores (`en_progreso`, `pendiente_aprobacion`) es responsabilidad del Supervisor.

---

#### Módulo Reportes

| Método | Endpoint | Permiso requerido | Asistente |
|--------|----------|-------------------|-----------|
| GET | `/api/v1/reportes` | `reportes:leer` | ✅ |
| GET | `/api/v1/reportes/{id}` | `reportes:leer` | ✅ |
| GET | `/api/v1/reportes/{id}/pdf` | `reportes:leer` | ✅ (descarga — cuenta como consulta) |
| POST | `/api/v1/reportes` | `reportes:crear` | ❌ → 403 |
| PATCH | `/api/v1/reportes/{id}` | `reportes:actualizar` | ❌ → 403 |

> **Impacto en créditos:** La descarga o visualización de un reporte por parte del Asistente incrementa el contador de consultas del tenant (conforme a RF-08.3). No consume crédito directo pero contribuye al excedente si se supera el umbral.

---

#### Endpoints Bloqueados (403 Forbidden)

| Endpoint | Motivo |
|----------|--------|
| `*/config_sistema/*` | Sin permiso de configuración |
| `*/aprobacion_docs/*` | Sin permiso de aprobación |
| `*/efirma/*` | Sin permiso de firma |
| `*/licencias/*` | Sin permiso de gestión de licencias |
| `*/creditos/*` | Sin permiso de gestión de créditos |
| `*/roles/*` | Sin permiso de gestión de usuarios |

**Respuesta estándar para acceso denegado:**
```json
{
  "error": "FORBIDDEN",
  "mensaje": "El rol ASISTENTE no tiene permiso para realizar esta acción.",
  "recurso": "config_sistema",
  "accion": "configurar",
  "codigo": 403
}
```

---

### 5.3 Webhooks / Eventos Relevantes

| Evento | Trigger | Payload clave | Quién escucha |
|--------|---------|--------------|---------------|
| `ticket.creado` | POST /tickets exitoso | `id`, `cliente_id`, `creado_por`, `estado` | Supervisor, Notificaciones |
| `cliente.registrado` | POST /clientes exitoso | `id`, `email`, `tenant_id` | CRM interno |
| `acceso.denegado` | 403 en cualquier endpoint | `usuario_id`, `recurso`, `accion`, `timestamp` | Seguridad, Auditoría |

---

## 6. Reglas de Negocio

| ID | Regla | Owner | Test ID |
|----|-------|-------|---------|
| RN-AST-01 | El rol Asistente solo puede leer, crear y actualizar clientes dentro de su propio tenant; nunca de otros tenants | Backend/Auth | TC-AST-01 |
| RN-AST-02 | El rol Asistente no puede eliminar clientes ni tickets; la eliminación requiere rol Supervisor o Admin CRM | Backend/Auth | TC-AST-02 |
| RN-AST-03 | El rol Asistente puede crear tickets únicamente en estado inicial `solicitado`; no puede crear tickets en estados intermedios | Backend | TC-AST-03 |
| RN-AST-04 | El rol Asistente puede actualizar campos informativos de un ticket (descripción, notas, prioridad, fecha_límite) mientras el ticket esté en estado `solicitado` o `asignado`; no puede modificar tickets en estado `pendiente_aprobacion`, `aprobado`, `entregado` o `cerrado` | Backend | TC-AST-04 |
| RN-AST-05 | Las únicas transiciones de estado que el Asistente puede ejecutar sobre un ticket son: `solicitado → cancelado` (cancelación por solicitud del cliente) y añadir comentarios/notas en cualquier estado | Backend | TC-AST-05 |
| RN-AST-06 | El rol Asistente puede consultar (GET) cualquier reporte del tenant, pero su acceso se contabiliza como consulta en el contador del tenant (RF-08.3) | Backend | TC-AST-06 |
| RN-AST-07 | El rol Asistente no puede generar nuevos reportes, modificar plantillas ni configurar secciones de reporte | Backend/Auth | TC-AST-07 |
| RN-AST-08 | Toda acción del Asistente (creación, actualización, acceso a reporte) queda registrada en el audit log inmutable con: `usuario_id`, `rol`, `accion`, `recurso_id`, `timestamp`, `ip_origen` | Backend | TC-AST-08 |
| RN-AST-09 | Un usuario puede tener asignado el rol Asistente en un tenant y otro rol (ej: Supervisor) en un tenant diferente; los permisos son siempre evaluados en el contexto del tenant del request | Auth | TC-AST-09 |
| RN-AST-10 | Si el Módulo de Tickets o el Módulo de Clientes está suspendido (licencia del tenant inactiva), el Asistente pierde acceso a esos módulos independientemente de sus permisos de rol | Backend | TC-AST-10 |

---

## 7. Flujos de Usuario (User Flows)

### 7.1 Registrar un Nuevo Cliente

```
Asistente → Navega a "Clientes" → Clic "Nuevo Cliente"
    → Formulario: nombre, email, teléfono, empresa, notas
    → [Validación OK?]
        SÍ → POST /clientes → Cliente creado → Confirmación en pantalla
        NO → Errores inline en el formulario
```

### 7.2 Crear un Ticket de Solicitud de Servicio

```
Asistente → Navega a "Tickets" → Clic "Nuevo Ticket"
    → Selecciona cliente existente (o crea uno nuevo — flujo 7.1)
    → Selecciona tipo de servicio, describe la solicitud, establece prioridad
    → [Opcional] Fecha límite
    → POST /tickets → Ticket creado en estado "Solicitado"
    → Notificación automática al Supervisor asignado al tenant
```

### 7.3 Consultar un Reporte Existente

```
Asistente → Navega a "Reportes"
    → Lista de reportes del tenant (GET /reportes)
    → Selecciona reporte → Vista detallada (GET /reportes/{id})
    → [Opcional] Descarga PDF (GET /reportes/{id}/pdf)
        → Sistema contabiliza +1 consulta al contador del tenant
```

### 7.4 Intento de Acceso a Función No Autorizada

```
Asistente → Intenta acceder a "Configuración del Sistema"
    → Middleware RBAC evalúa: rol=ASISTENTE, recurso=config_sistema, accion=leer
    → Permiso: DENEGADO
    → Pantalla de bloqueo: "No tienes permiso para acceder a esta sección.
      Contacta al administrador de tu cuenta si necesitas acceso."
    → Log de auditoría registra el intento denegado
```

---

## 8. Arquitectura Técnica

### 8.1 Componentes Afectados

| Componente | Rol | Cambio requerido |
|------------|-----|-----------------|
| `auth-service` | Emisión y validación de JWT con claim de rol | Añadir `rol: "ASISTENTE"` como valor válido |
| `rbac-middleware` | Evaluación de permisos en cada request | Cargar matriz de permisos del rol Asistente; evaluar antes de llegar al controlador |
| `clientes-service` | CRUD de clientes | Aplicar restricción: DELETE bloqueado para rol Asistente |
| `tickets-service` | CRUD y transiciones de tickets | Aplicar restricciones RN-AST-03, RN-AST-04, RN-AST-05 |
| `reportes-service` | Consulta de reportes | Permitir GET; registrar +1 consulta en contador del tenant |
| `auditoria-service` | Registro inmutable de acciones | Capturar toda acción del Asistente y accesos denegados |
| `notificaciones-service` | Alertas por eventos | Notificar al Supervisor cuando el Asistente crea un ticket |

### 8.2 Evaluación de Permisos — Pseudocódigo

```python
def authorize(request, required_permission):
    token = parse_jwt(request.headers["Authorization"])
    rol   = token.claims["rol"]
    tenant = token.claims["tenant_id"]

    # Verificar que el recurso pertenece al mismo tenant
    if not resource_belongs_to_tenant(request.resource_id, tenant):
        raise ForbiddenError("cross_tenant_access")

    # Evaluar permiso en matriz RBAC
    permission = db.query(RolPermiso).filter(
        rol=rol,
        recurso=required_permission.recurso,
        accion=required_permission.accion
    ).first()

    if not permission or not permission.concedido:
        audit_log(token.sub, rol, "denegado", required_permission, request.ip)
        raise ForbiddenError("permission_denied")

    audit_log(token.sub, rol, "permitido", required_permission, request.ip)
    return True
```

### 8.3 Decisiones de Diseño (ADRs)

| # | Decisión | Alternativa descartada | Razón |
|---|----------|----------------------|-------|
| ADR-AST-01 | Deny explícito en BD para recursos críticos (config, aprobación) | Solo ausencia de permiso positivo | Un deny explícito es más seguro: si por error se inserta un allow, el deny tiene precedencia |
| ADR-AST-02 | Permisos evaluados en backend (middleware), nunca solo en frontend | Ocultar botones en UI como única medida | La UI es bypasseable; la autorización debe vivir en el servidor |
| ADR-AST-03 | Rol como claim en JWT, validado en cada request | Consultar rol a BD en cada request | Latencia: el JWT evita un roundtrip a BD en cada llamada. TTL corto (15 min) para mitigar permisos revocados |

---

## 9. Seguridad y Compliance

| Área | Requisito | Implementación |
|------|-----------|----------------|
| Autenticación | El Asistente debe autenticarse con JWT válido en cada request | JWT firmado por `auth-service`; TTL 15 min; refresh token 8h |
| Autorización | El rol Asistente no puede acceder a recursos fuera de su matriz de permisos | `rbac-middleware` evalúa antes del controlador; 403 + audit log en caso de denegación |
| Confinamiento de tenant | El Asistente solo opera sobre datos de su tenant | `tenant_id` del JWT se valida contra el `tenant_id` del recurso solicitado en cada query |
| Auditoría | Toda acción y acceso denegado quedan en log inmutable | `auditoria-service` append-only; campos: `usuario_id`, `rol`, `accion`, `recurso_id`, `resultado`, `ip_origen`, `timestamp` |
| Elevación de privilegios | El Asistente no puede asignarse un rol de mayor privilegio | Los endpoints de gestión de roles requieren `roles:gestionar`; Asistente no lo tiene |
| Datos sensibles | El Asistente ve datos de clientes del tenant; aplica política de mínimo privilegio | Solo campos necesarios para operar; no tiene acceso a datos de pago ni información financiera |

---

## 10. Rendimiento y Escalabilidad

| Escenario | Volumen esperado | SLA | Estrategia |
|-----------|-----------------|-----|------------|
| Evaluación de permiso RBAC en cada request | Mismo volumen que requests generales (~500 req/s pico) | < 5 ms adicionales al p99 del endpoint | Cache de matriz de permisos por rol en Redis; TTL 5 min; invalidar al cambiar permisos |
| Audit log de acciones del Asistente | Proporcional a la actividad operativa | Escritura asíncrona sin bloquear response | Queue de eventos → `auditoria-service` consume en background |

---

## 11. Plan de Pruebas

### 11.1 Casos de Prueba Clave

| ID | Escenario | Precondición | Resultado Esperado |
|----|-----------|-------------|-------------------|
| TC-AST-01 | Asistente intenta leer cliente de otro tenant | Token con `tenant_id=A`; cliente pertenece a `tenant_id=B` | HTTP 403 — cross_tenant_access |
| TC-AST-02 | Asistente intenta eliminar un cliente | Cliente existente en el tenant del Asistente | HTTP 403 — permission_denied |
| TC-AST-03 | Asistente crea ticket en estado distinto a `solicitado` | Payload con `estado: "en_progreso"` | HTTP 400 — estado_inicial_invalido (forzado a `solicitado`) |
| TC-AST-04 | Asistente actualiza ticket en estado `pendiente_aprobacion` | Ticket en estado `pendiente_aprobacion` | HTTP 403 — ticket_estado_bloqueado |
| TC-AST-05 | Asistente cancela ticket en estado `solicitado` | Ticket en estado `solicitado` | HTTP 200 — estado cambia a `cancelado` |
| TC-AST-06 | Asistente descarga PDF de un reporte | Reporte existente en su tenant | HTTP 200 + PDF; contador de consultas del tenant +1 |
| TC-AST-07 | Asistente intenta crear un nuevo reporte | POST /reportes con payload válido | HTTP 403 — permission_denied |
| TC-AST-08 | Acción del Asistente queda en audit log | Cualquier acción exitosa | Registro en `auditoria_acciones` con todos los campos requeridos |
| TC-AST-09 | Asistente intenta acceder a Configuración del Sistema | GET /config_sistema | HTTP 403 — permission_denied + pantalla de bloqueo en UI |
| TC-AST-10 | Módulo de Tickets suspendido; Asistente intenta acceder | Licencia del tenant en estado `suspendida` | HTTP 403 — modulo_suspendido |

### 11.2 Estrategia de Testing

| Tipo | Cobertura mínima | Herramienta |
|------|-----------------|-------------|
| Unit tests | Todas las reglas RN-AST-* | Jest / pytest |
| Integration tests | Flujos end-to-end de TC-AST-01 a TC-AST-10 | Supertest / Postman |
| Security tests | Intentos de privilege escalation y cross-tenant access | OWASP ZAP / tests manuales |
| Contract tests | Respuestas 403 con estructura estándar de error | Pact |

---

## 12. Plan de Implementación

| Fase | Entregable | Estimación | Dependencias |
|------|-----------|-----------|--------------|
| 1 — Modelo RBAC | Migraciones: tablas `roles`, `permisos`, `rol_permiso`, `usuario_rol` | 1 día | — |
| 2 — Seed de permisos | Seed del rol ASISTENTE con su matriz de permisos | 0.5 días | Fase 1 |
| 3 — Middleware RBAC | Evaluación de permisos en cada request; deny explícito; 403 estándar | 2 días | Fase 1, 2 |
| 4 — Restricciones de servicio | Reglas RN-AST-03, 04, 05 en `tickets-service`; bloqueo DELETE en `clientes-service` | 2 días | Fase 3 |
| 5 — Audit log | Captura de acciones y accesos denegados del Asistente | 1 día | Fase 3 |
| 6 — UI — Pantallas de bloqueo | Mensaje de acceso denegado en módulos restringidos (config, aprobación) | 1 día | Fase 3 |
| 7 — QA & Tests de seguridad | Ejecución de TC-AST-01 a TC-AST-10; pruebas de penetración básicas | 2 días | Fases 1-6 |

**Total estimado:** ~9.5 días de desarrollo.

---

## 13. Preguntas Abiertas y Decisiones Pendientes

| # | Pregunta | Owner | Deadline | Estado |
|---|----------|-------|----------|--------|
| Q-AST-01 | ¿El Asistente puede reasignar tickets (cambiar el técnico asignado) o eso es exclusivo del Supervisor? | Product | — | Abierta |
| Q-AST-02 | ¿El Asistente puede exportar el listado de clientes a CSV/Excel o solo consultar en pantalla? | Product | — | Abierta |
| Q-AST-03 | ¿El Asistente puede ver todos los reportes del tenant o solo los asociados a los tickets que él creó? | Product | — | Abierta |
| Q-AST-04 | ¿El rol Asistente tiene acceso al historial de auditoría de sus propias acciones, o ese historial es solo visible para el Supervisor/Admin? | Product | — | Abierta |
| Q-AST-05 | ¿Se necesita un flujo de notificación al Supervisor cuando el Asistente cancela un ticket (RN-AST-05)? | Product | — | Abierta |

---

## 14. Glosario

| Término | Definición |
|---------|-----------|
| Asistente | Rol operativo del Mikel CRM con permisos de gestión de clientes y tickets, y consulta de reportes. Sin acceso a configuración ni aprobación de documentos |
| RBAC | Role-Based Access Control — modelo de control de acceso donde los permisos se asignan a roles y los roles a usuarios |
| Deny explícito | Registro de permiso con `concedido = false` que bloquea una acción independientemente de otros allows |
| Confinamiento de tenant | Restricción que impide que un usuario acceda a datos de un tenant diferente al suyo |
| Middleware de autorización | Componente de software que intercepta cada request HTTP y verifica los permisos antes de enrutar al controlador de negocio |
| Audit log | Registro inmutable y cronológico de todas las acciones realizadas en el sistema, incluyendo los accesos denegados |
| JWT | JSON Web Token — token firmado que contiene los claims del usuario (identidad, rol, tenant) y es validado en cada request |

---

## 15. Anexos

- [ ] Diagrama de secuencia — flujo de autorización en middleware RBAC
- [ ] Mockup — pantalla de bloqueo de acceso denegado para el Asistente
- [ ] Referencia cruzada con `REQUERIMIENTOS_HISTORIAS_USUARIO.md` — actores relacionados
- [ ] ADR completos para decisiones ADR-AST-01 a ADR-AST-03

---

> **Checklist de aprobación:**
> - [ ] Product Owner revisó y aprobó alcance y permisos
> - [ ] Tech Lead revisó arquitectura RBAC y decisiones ADR
> - [ ] QA revisó plan de pruebas y casos TC-AST-*
> - [ ] Security revisó sección 9 (seguridad y confinamiento de tenant)
> - [ ] Estimación validada por el equipo de desarrollo
