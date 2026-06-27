# Plan de Implementación: License Service Microservice

## Resumen

Implementación incremental del microservicio `license-service` usando Java 21, Spring Boot 3.x, PostgreSQL 16, Redis 7, Kafka y Flyway. El plan sigue 10 fases progresivas donde cada paso construye sobre los anteriores, finalizando con la integración completa. Los property-based tests utilizan jqwik integrado con JUnit 5.

## Tasks

- [x] 1. Infraestructura base — Spring Boot + PostgreSQL + Redis + Kafka
  - [x] 1.1 Crear proyecto Spring Boot 3.x con Java 21
    - Inicializar estructura Maven/Gradle con dependencias: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-kafka, flyway-core, postgresql driver, jqwik, testcontainers
    - Configurar `application.yml` con perfiles (local, test, prod)
    - Configurar HikariCP con max 20 conexiones
    - _Requirements: 14.3, 15.5_

  - [x] 1.2 Configurar Docker Compose para entorno local
    - PostgreSQL 16, Redis 7, Kafka (con Zookeeper o KRaft), pgAdmin
    - Volúmenes persistentes y health checks
    - _Requirements: 14.3_

  - [x] 1.3 Configurar Spring Security con validación JWT RS256
    - Implementar `JwtAuthFilter` que valide firma RS256, TTL 15 min y extraiga claims `tenantId`, `userId`, `rol`
    - Configurar `SecurityFilterChain` con rutas públicas (health) y protegidas
    - _Requirements: 15.1, 15.2_

  - [x] 1.4 Configurar Rate Limiting con Redis sliding window
    - Implementar filtro de rate limiting: Query API 1000 req/s por Tenant, Command API 100 req/s por Tenant
    - Responder HTTP 429 cuando se exceda el límite
    - _Requirements: 15.3_

- [x] 2. Modelo de datos — Migraciones Flyway
  - [x] 2.1 Crear migración Flyway V1: tablas principales
    - Tablas: `tenant`, `contrato_anual`, `cuota_mensual`, `renovacion`, `cuota_almacenamiento`
    - Constraints: ENUMs para estados, CHECK para campos, FK relationships
    - Campos monetarios como `NUMERIC(10,2)`
    - _Requirements: 14.1, 14.2, 14.5_

  - [x] 2.2 Crear migración Flyway V2: tablas de créditos y contadores
    - Tablas: `paquete_creditos`, `evento_credito`, `contador_consultas`
    - CHECK constraint: `saldo_disponible >= 0`, `saldo_resultante >= 0`
    - _Requirements: 14.1, 14.2, 5.5_

  - [x] 2.3 Crear migración Flyway V3: RBAC y Audit Log
    - Tablas: `rol_usuario`, `audit_log` (particionada por mes)
    - Tabla `audit_log` como append-only con trigger que bloquee UPDATE/DELETE
    - Crear primera partición de ejemplo
    - _Requirements: 14.1, 13.2, 13.3_

  - [x] 2.4 Crear migración Flyway V4: índices optimizados
    - Índices: `idx_contrato_tenant_estado`, `idx_cuota_fecha_estado`, `idx_contrato_vencimiento_auto`, `idx_contador_tenant_periodo`
    - Usar CREATE INDEX CONCURRENTLY
    - _Requirements: 14.4_

  - [x] 2.5 Crear entidades JPA correspondientes a todas las tablas
    - Mapear enums de estado con `@Enumerated(STRING)`
    - Usar `BigDecimal` para todos los campos monetarios y de créditos
    - Implementar repositorios Spring Data JPA
    - _Requirements: 14.1, 14.5_

- [x] 3. Checkpoint — Verificar infraestructura y modelo de datos
  - Ejecutar migraciones Flyway contra PostgreSQL local
  - Verificar que todas las constraints y índices se crean correctamente
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Query API — AccessQueryService + Redis Cache
  - [x] 4.1 Implementar AccessQueryService con caché Redis
    - Endpoint `GET /api/v1/access/{tenantId}`: leer clave `tenant:{tenantId}:access` de Redis
    - En cache hit: deserializar JSON y evaluar permisos → responder en < 5 ms
    - En cache miss: consultar PostgreSQL, serializar en Redis con TTL 300s, responder
    - Responder HTTP 403 con detalle de adeudo si Tenant está SUSPENDED
    - _Requirements: 8.1, 8.2, 8.3, 8.5_

  - [x] 4.2 Implementar ContractQueryService
    - Endpoint `GET /api/v1/tenants/{tenantId}/contracts`: listar contratos con estado, cuotas pagadas, descuento disponible hoy
    - Endpoint `GET /api/v1/tenants/{tenantId}/credits`: saldo, historial, contadores, umbral, excedente
    - Endpoint `GET /api/v1/tenants/{tenantId}/reactivation-summary`: desglose del adeudo
    - _Requirements: 8.1, 1.4_

  - [x] 4.3 Implementar invalidación de caché Redis
    - Método `invalidateAccessCache(UUID tenantId)` que elimina la clave Redis
    - Invocar en cada Command que modifique estado del Tenant
    - _Requirements: 8.4_

  - [ ]* 4.4 Write property test: invalidación de caché (Property 20)
    - **Property 20: Invalidación de caché Redis en cada Command de estado**
    - Para cualquier Command exitoso que modifique estado, la clave Redis SHALL ser eliminada
    - **Validates: Requirements 8.4**

- [x] 5. Command API core — Contratos, cuotas, pagos, descuentos
  - [x] 5.1 Implementar gestión del ciclo de vida del Tenant
    - `POST /api/v1/tenants`: crear Tenant en ONBOARDING
    - Implementar máquina de estados con transiciones permitidas: {ONBOARDING→ACTIVE, ACTIVE→SUSPENDED, SUSPENDED→ACTIVE, ACTIVE→CANCELLED, SUSPENDED→CANCELLED}
    - Rechazar transiciones inválidas con HTTP 422
    - Publicar eventos de transición a Kafka
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [ ]* 5.2 Write property test: máquina de estados (Property 9)
    - **Property 9: Máquina de estados solo permite transiciones válidas**
    - Generar pares (estadoActual, transiciónIntentada) y verificar aceptación/rechazo
    - **Validates: Requirements 1.7**

  - [x] 5.3 Implementar creación y gestión de Contratos Anuales
    - `POST /api/v1/tenants/{tenantId}/contracts`: crear contrato con tipo, módulo, cuota fija, 12 cuotas
    - Validar unicidad de contrato ACTIVE por módulo (HTTP 409)
    - Upgrade: crear nuevo contrato independiente
    - Downgrade: marcar como no renovable, efectivo al vencimiento
    - Rechazar baja si hay cuotas vencidas (HTTP 422)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

  - [ ]* 5.4 Write property test: unicidad de contrato activo (Property 11)
    - **Property 11: Unicidad de contrato activo por módulo**
    - Intentar crear segundo contrato ACTIVE para mismo módulo → HTTP 409
    - **Validates: Requirements 2.5**

  - [ ]* 5.5 Write property test: inmutabilidad de cuota (Property 12)
    - **Property 12: Inmutabilidad de cuota mensual durante vigencia del contrato**
    - Para cualquier contrato creado con cuota C, el valor permanece C en las 12 cuotas
    - **Validates: Requirements 2.4**

  - [x] 5.6 Implementar registro de pagos con descuento por puntualidad
    - `POST /api/v1/tenants/{tenantId}/contracts/{contratoId}/cuotas/{cuotaId}/pay`
    - Calcular descuento: 10% (antes de fecha límite), 3% (mismo día), 0% (día +1 gracia)
    - Lanzar `CuotaVencidaException` si `fechaPago > fechaLimite + 1`
    - Registrar monto cobrado con descuento aplicado
    - _Requirements: 3.1, 3.2, 3.3, 9.2_

  - [ ]* 5.7 Write property test: descuento por puntualidad (Property 1)
    - **Property 1: Descuento por puntualidad es determinista según fecha relativa**
    - Generar pares (fechaPago, fechaLimite) con BigDecimal positivo y verificar cálculo exacto
    - **Validates: Requirements 3.1, 3.2, 3.3, 9.2**

  - [x] 5.8 Implementar reactivación de Tenant suspendido
    - `POST /api/v1/tenants/{tenantId}/reactivate`
    - Calcular adeudo total: cuotas vencidas + cuotas de almacenamiento acumuladas
    - Validar que `montoEsperado` == adeudo total exacto (rechazar si difiere)
    - Restaurar acceso + invalidar Redis
    - _Requirements: 1.4, 3.7, 9.4_

  - [ ]* 5.9 Write property test: monto exacto de reactivación (Property 10)
    - **Property 10: Reactivación requiere monto exacto igual al adeudo total**
    - Generar adeudos y montos ofrecidos; aceptar solo si son numéricamente iguales
    - **Validates: Requirements 1.4, 3.7, 9.4**

  - [x] 5.10 Implementar renovación anual de contratos
    - `POST /api/v1/tenants/{tenantId}/contracts/{contratoId}/renew`
    - Validar ventana de renovación (días -30 a 0) o estado EXPIRED
    - Cobrar tarifa 99€ + primera cuota del nuevo período (con descuento si aplica)
    - Crear nuevo ContratoAnual con fechas +12 meses
    - Publicar `contrato.renewed`
    - _Requirements: 4.3, 4.4, 9.3_

  - [ ]* 5.11 Write property test: tarifa renovación independiente (Property 19)
    - **Property 19: Tarifa de renovación es independiente por contrato**
    - Para N contratos venciendo el mismo día, total tarifas = N × 99€
    - **Validates: Requirements 4.4**

- [x] 6. Checkpoint — Verificar Query API y Command API core
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Créditos — Consumo, bonus, contadores, excedente
  - [x] 7.1 Implementar adquisición de paquete de créditos
    - `POST /api/v1/tenants/{tenantId}/credits/packages`
    - Crear PaqueteCreditos con N créditos + 4 bonus automáticos
    - Saldo disponible = N + 4; fecha vencimiento = hoy + 1 año
    - _Requirements: 5.6, 5.8_

  - [ ]* 7.2 Write property test: adquisición con bonus (Property 6)
    - **Property 6: Adquisición de paquete otorga créditos + 4 bonus**
    - Para cualquier N > 0, saldo resultante incrementa en exactamente N + 4
    - **Validates: Requirements 5.6**

  - [x] 7.3 Implementar consumo de créditos con transacción SERIALIZABLE
    - `POST /api/v1/tenants/{tenantId}/credits/consume`
    - Costos: ASISTENCIA_DIGITAL = 1.0, ASISTENTE_AUTOMATICO_NORMAS = 1.5
    - SELECT FOR UPDATE con isolation SERIALIZABLE
    - Rechazar con HTTP 402 si saldo < costo
    - Registrar EventoCredito con todos los campos requeridos
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.7, 9.5_

  - [ ]* 7.4 Write property test: consumo por perfil (Property 3)
    - **Property 3: Consumo de créditos por perfil de documento**
    - Para cualquier saldo suficiente y perfil válido, descuento = costo exacto del perfil
    - **Validates: Requirements 5.1, 5.2**

  - [ ]* 7.5 Write property test: rechazo por saldo insuficiente (Property 5)
    - **Property 5: Rechazo por saldo insuficiente**
    - Si saldo < costo_perfil → HTTP 402 y saldo_despues == saldo_antes
    - **Validates: Requirements 5.4**

  - [ ]* 7.6 Write property test: saldo nunca negativo (Property 17)
    - **Property 17: Saldo de créditos nunca es negativo**
    - Para cualquier secuencia de operaciones, saldo_disponible >= 0 siempre
    - **Validates: Requirements 14.2**

  - [x] 7.7 Implementar compensación de créditos (round-trip)
    - Si generación de PDF falla post-reserva, reintegrar créditos en ≤ 30 segundos
    - Registrar EventoCredito de tipo COMPENSACION
    - _Requirements: 5.3_

  - [ ]* 7.8 Write property test: compensación round-trip (Property 4)
    - **Property 4: Compensación de créditos es un round-trip**
    - Reservar → fallar → compensar = saldo original
    - **Validates: Requirements 5.3**

  - [x] 7.9 Implementar contadores de consultas y excedente
    - `POST /api/v1/tenants/{tenantId}/consultations`
    - Incrementar contador global y contador por tipo de reporte
    - Calcular umbral: créditos_totales_adquiridos × 100
    - Cobrar 1 crédito por cada bloque completo de 100 consultas de excedente
    - Si saldo = 0: registrar deuda sin bloquear consultas
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.9_

  - [ ]* 7.10 Write property test: contadores monotónicos (Property 7)
    - **Property 7: Contadores de consultas son monotónicamente crecientes**
    - Para N consultas, contador global = N; contadores por tipo suman = N
    - **Validates: Requirements 6.3**

  - [ ]* 7.11 Write property test: umbral y excedente (Property 8)
    - **Property 8: Cálculo de umbral y cobro de excedente por bloques de 100**
    - Umbral = créditos × 100; cobro = floor((G - umbral) / 100)
    - **Validates: Requirements 6.4, 6.5, 6.6**

  - [x] 7.12 Implementar alertas de saldo bajo
    - Publicar `credito.balance_low` cuando saldo cruza umbrales 20%, 10%, 0%
    - Un solo evento por cruce de umbral
    - _Requirements: 10.4_

  - [ ]* 7.13 Write property test: alertas umbral (Property 18)
    - **Property 18: Alertas de saldo bajo se disparan en umbrales correctos**
    - Verificar que se dispara exactamente un evento por cruce de umbral
    - **Validates: Requirements 10.4**

- [x] 8. Checkpoint — Verificar sistema de créditos completo
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Cron Jobs — Los 6 jobs programados
  - [x] 9.1 Implementar CuotaOverdueJob (diario 1:00 AM)
    - Detectar cuotas con fecha_limite + 2 días sin pago
    - Publicar `cuota.overdue` → disparar SuspenderTenantCommand
    - Batch de 500 registros con procesamiento asíncrono
    - _Requirements: 11.1, 3.4_

  - [ ]* 9.2 Write property test: fórmula almacenamiento (Property 2)
    - **Property 2: Fórmula de Cuota de Almacenamiento**
    - Para cualquier reportes >= 0: resultado = 50 + CEIL(MAX(rep - 100, 0) / 100) × 10
    - **Validates: Requirements 3.5**

  - [x] 9.3 Implementar RenovacionNotificacionJob (diario 8:00 AM)
    - Buscar contratos con vencimiento - hoy ∈ {30, 7, 1} días
    - Publicar eventos de notificación con tarifa 99€ y opciones
    - _Requirements: 11.2, 4.1, 4.2_

  - [x] 9.4 Implementar RenovacionAutoJob (diario 9:00 AM)
    - Procesar renovación automática de contratos que vencen hoy con `renovacion_auto = true`
    - Cobro falla → publicar `cuota.overdue`
    - _Requirements: 11.3, 4.3, 4.6_

  - [x] 9.5 Implementar CuotaAlmacenamientoJob (día 1 cada mes, 2:00 AM)
    - Generar Cuota Almacenamiento para Tenants SUSPENDED
    - Calcular: 50 + CEIL(MAX(reportes - 100, 0) / 100) × 10
    - _Requirements: 11.4, 3.5, 3.6_

  - [x] 9.6 Implementar CreditosVencimientoJob (diario 3:00 AM)
    - Caducar paquetes con fecha_vencimiento = ayer
    - Saldo a 0; publicar `credito.expired`
    - _Requirements: 11.5, 5.8_

  - [x] 9.7 Implementar TenantCancelacionJob (diario 4:00 AM)
    - Cancelar Tenants SUSPENDED >= 90 días sin pago
    - Publicar `tenant.cancelled`
    - _Requirements: 11.6, 1.6_

- [x] 10. Eventos — Publicación Kafka + Event Handlers
  - [x] 10.1 Implementar infraestructura de Domain Events
    - Clase base `DomainEvent` con schema: eventId, eventType, version, tenantId, occurredAt, payload, metadata
    - Serialización JSON con Jackson
    - Producer Kafka al topic `license-events` con retry + backoff exponencial + dead letter queue
    - _Requirements: 10.1, 10.2, 10.3_

  - [ ]* 10.2 Write property test: schema de eventos (Property 16)
    - **Property 16: Schema de Domain Events es completo y consistente**
    - Todo evento serializado contiene todos los campos requeridos del schema base
    - **Validates: Requirements 10.2**

  - [x] 10.3 Implementar PaymentEventHandler (consumer Kafka)
    - Consumir `payment.confirmed` y `payment.failed` del Payment Service
    - Procesar confirmaciones de apertura → transicionar Tenant a ACTIVE
    - Procesar pagos de cuotas → actualizar estado y aplicar descuento
    - Procesar fallos → registrar intento fallido
    - _Requirements: 1.2, 9.2_

  - [x] 10.4 Implementar catálogo completo de eventos publicados
    - Todos los 16 tipos de eventos definidos en el diseño
    - Verificar que cada transición de estado publica su evento correspondiente
    - _Requirements: 10.1_

- [x] 11. Checkpoint — Verificar Cron Jobs y Eventos
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. RBAC — Roles, permisos, middleware
  - [x] 12.1 Implementar matriz de permisos por rol
    - Definir 4 roles: ADMIN_CUENTA, SUPERVISOR, TECNICO, ASISTENTE
    - Tabla de permisos: (rol, recurso, acción) → concedido (boolean)
    - Deny explícito tiene precedencia sobre allow
    - Cache de matriz en Redis con TTL 5 min
    - _Requirements: 12.1, 12.2, 12.6_

  - [x] 12.2 Implementar RBACMiddleware
    - Evaluar permisos en cada request antes de ejecutar lógica
    - Validar confinamiento de Tenant: `tenantId` del JWT == `tenantId` del recurso
    - Denegar acceso cross-tenant con HTTP 403
    - Denegar acceso a módulos suspendidos independientemente del rol
    - Registrar accesos denegados en Audit Log
    - _Requirements: 12.2, 12.3, 12.4, 12.5_

  - [ ]* 12.3 Write property test: RBAC consistente con matriz (Property 13)
    - **Property 13: Evaluación RBAC es consistente con la matriz de permisos**
    - Para cualquier (rol, recurso, acción), resultado = lo definido en la matriz
    - **Validates: Requirements 12.1, 12.2, 12.3**

  - [ ]* 12.4 Write property test: aislamiento de Tenant (Property 14)
    - **Property 14: Aislamiento de Tenant (confinamiento)**
    - Para cualquier tenantId_JWT != tenantId_recurso → HTTP 403
    - **Validates: Requirements 12.4, 15.2**

  - [ ]* 12.5 Write property test: módulo suspendido deniega acceso (Property 15)
    - **Property 15: Módulo suspendido deniega acceso independientemente del rol**
    - Para cualquier rol y módulo suspendido → HTTP 403
    - **Validates: Requirements 12.5**

- [x] 13. Audit Log — AOP @AuditAction + tabla particionada
  - [x] 13.1 Implementar AuditService con AOP
    - Anotación `@AuditAction` aplicable a métodos de Commands
    - Interceptor AOP que captura: tenant_id, usuario_id, acción, entidad, entidad_id, payload_antes, payload_despues, resultado, ip_origen, timestamp
    - Escritura asíncrona a tabla `audit_log` particionada
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 13.2 Implementar registro de accesos denegados
    - Registrar con resultado DENEGADO y motivo específico
    - Misma estructura que acciones exitosas
    - _Requirements: 13.4, 13.5_

- [x] 14. Apertura de Contrato (Onboarding)
  - [x] 14.1 Implementar flujo de Apertura de Contrato
    - Apertura Estándar (400€): 2 créditos bienvenida, modalidad ESTANDAR
    - Apertura Personalizada (1.400€): 10 créditos bienvenida, modalidad PERSONALIZADO
    - Créditos de bienvenida no caducan antes de 12 meses
    - Upgrade Estándar → Personalizada: 1.000€ diferencia + 8 créditos adicionales
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 15. Checkpoint — Verificar RBAC, Audit Log y Onboarding
  - Ensure all tests pass, ask the user if questions arise.

- [x] 16. Integración y cableado final
  - [x] 16.1 Integrar todos los componentes end-to-end
    - Verificar que Commands invalidan Redis correctamente
    - Verificar que eventos Kafka se publican en todas las transiciones
    - Verificar que Audit Log registra todas las operaciones
    - Verificar que RBAC se evalúa antes de cada Command/Query
    - _Requirements: 8.4, 9.6, 10.1, 12.2, 13.1_

  - [x] 16.2 Implementar warmup de caché Redis al iniciar servicio
    - Pre-cargar Tenants ACTIVE con contratos próximos a vencer
    - _Requirements: 8.1_

  - [ ]* 16.3 Write integration tests end-to-end
    - Test flujo completo: onboarding → pago apertura → activación → contrato → cuota → créditos → suspensión → reactivación
    - Usar Testcontainers con PostgreSQL, Redis y Kafka
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 5.1, 8.1_

- [x] 17. Checkpoint final — Verificar integración completa
  - Ensure all tests pass, ask the user if questions arise.

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia los requerimientos específicos para trazabilidad
- Los checkpoints aseguran validación incremental entre fases
- Los property tests validan las 20 propiedades de correctness definidas en el diseño usando jqwik
- Los unit tests validan ejemplos específicos y condiciones de borde (complementarios a los property tests)
- Todos los campos monetarios usan `BigDecimal` en Java y `NUMERIC(10,2)` en PostgreSQL
- Las transacciones de créditos usan isolation level SERIALIZABLE con SELECT FOR UPDATE
