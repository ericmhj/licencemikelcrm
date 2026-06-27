# Documento de Requerimientos

## Introducción

Este documento define los requerimientos del microservicio `license-service` para la plataforma Mikel CRM. El microservicio gestiona de forma independiente el ciclo de vida completo de licencias, contratos anuales, créditos, contadores de consultas, política de pagos y control de acceso basado en roles (RBAC) para todos los tenants del sistema SaaS.

La arquitectura implementa el patrón **CQRS** (Command Query Responsibility Segregation) con caché Redis para validación ultrarrápida de acceso (<5 ms p99) y PostgreSQL como fuente de verdad. Publica Domain Events al bus de mensajería (Kafka) para integración asíncrona con otros microservicios.

**Stack tecnológico:** Java 21 · Spring Boot 3.x · PostgreSQL 16 · Redis 7 · Kafka

---

## Glosario

- **License_Service**: Microservicio independiente que gestiona contratos, créditos, contadores, pagos y control de acceso para el Mikel CRM
- **Tenant**: Organización cliente que opera en una instancia lógicamente aislada; unidad base de toda la gestión de licencias
- **Contrato_Anual**: Único modelo de contratación; compromiso de 12 meses con pago fraccionado en 12 cuotas mensuales
- **Cuota_Mensual**: Pago periódico equivalente a 1/12 del valor anual del contrato, cobrado en la Fecha_de_Aniversario
- **Fecha_de_Aniversario**: Día del mes en que se cobra la cuota mensual, igual al día de inicio del contrato
- **Fecha_Limite**: Fecha de vencimiento de una cuota; día 0 de la ventana de pago
- **Ventana_de_Renovacion**: Período de 30 días previos a la Fecha de Vencimiento del Contrato para gestionar la renovación
- **Cuota_de_Almacenamiento**: Cargo mensual facturado al Tenant durante suspensión por mora; base 50 €/mes para ≤100 reportes, +10 € por cada 100 reportes adicionales
- **Credito**: Unidad de consumo del sistema con 2 decimales de precisión (BigDecimal)
- **Perfil_de_Documento**: Categoría que determina el costo en créditos al generar un PDF: Asistencia_Digital (1.0) o Asistente_Automatico_de_Normas (1.5)
- **Evento_de_Credito**: Evento atómico de descuento del saldo al generar un PDF exitosamente
- **Paquete_de_Creditos**: Conjunto de créditos adquiridos con vigencia de 12 meses
- **Contador_Global_Consultas**: Acumulador maestro por Tenant que suma consultas de todos los tipos de reporte
- **Umbral_de_Consultas_Incluidas**: Número de consultas gratuitas = créditos_totales_adquiridos × 100
- **Apertura_de_Contrato**: Pago único obligatorio que activa el CRM y determina la modalidad de reporte
- **Query_API**: Componente de lectura del CQRS que valida acceso vía caché Redis
- **Command_API**: Componente de escritura del CQRS que ejecuta transiciones de estado en PostgreSQL
- **Domain_Event**: Hecho inmutable del dominio publicado al bus Kafka
- **Audit_Log**: Registro inmutable append-only de todas las transiciones de estado
- **RBAC**: Control de acceso basado en roles con 4 roles: ADMIN_CUENTA, SUPERVISOR, TECNICO, ASISTENTE
- **Redis_Cache**: Caché de acceso con TTL de 300 segundos e invalidación por eventos
- **Cron_Job**: Proceso programado que ejecuta lógica de negocio en horarios definidos

---

## Requerimientos

### Requerimiento 1: Gestión del Ciclo de Vida del Tenant

**User Story:** Como plataforma Mikel CRM, quiero gestionar el ciclo de vida completo de cada Tenant (onboarding → active → suspended → reactivated → cancelled), para asegurar que las transiciones de estado se ejecuten conforme a las reglas de negocio y se propaguen a todos los servicios consumidores.

#### Criterios de Aceptación

1. WHEN un nuevo Tenant se registra con modalidad de apertura válida (ESTANDAR o PERSONALIZADO), THE License_Service SHALL crear el Tenant en estado ONBOARDING y publicar el evento `tenant.onboarded` con tenantId, modalidad y créditos de bienvenida
2. WHEN se confirma el pago de la Apertura de Contrato, THE License_Service SHALL transicionar el Tenant de ONBOARDING a ACTIVE, activar los créditos de bienvenida (2 para Estándar, 10 para Personalizado) y publicar el evento `tenant.activated`
3. WHEN una cuota mensual alcanza el día +2 sin pago confirmado, THE License_Service SHALL transicionar el Tenant de ACTIVE a SUSPENDED, bloquear el acceso operativo e invalidar la caché Redis del Tenant
4. WHEN se recibe el pago total de reactivación (cuotas vencidas + Cuotas de Almacenamiento acumuladas), THE License_Service SHALL transicionar el Tenant de SUSPENDED a ACTIVE y restaurar el acceso en menos de 60 segundos
5. WHEN el administrador solicita la cancelación y no existe adeudo pendiente, THE License_Service SHALL transicionar el Tenant de ACTIVE a CANCELLED, iniciar el período de retención de 90 días y publicar el evento `tenant.cancelled`
6. WHILE un Tenant permanece en estado SUSPENDED por 90 días o más sin pago, THE License_Service SHALL transicionarlo automáticamente a CANCELLED
7. IF se intenta una transición de estado no permitida por la máquina de estados, THEN THE License_Service SHALL rechazar la operación con código HTTP 422 y un mensaje descriptivo del error

---

### Requerimiento 2: Contratos Anuales con Cuotas Mensuales

**User Story:** Como Cliente del Mikel CRM, quiero formalizar contratos anuales de 12 meses con pago fraccionado en cuotas mensuales, para tener un compromiso claro de permanencia y pagos predecibles.

#### Criterios de Aceptación

1. THE License_Service SHALL soportar exclusivamente el modelo de Contrato Anual con 12 cuotas mensuales iguales; no existe modalidad mensual renovable
2. WHEN se crea un nuevo contrato anual, THE License_Service SHALL registrar: tipo (MODULO o CREDITOS), módulo asociado, cuota mensual fija, fecha de inicio, fecha de vencimiento (inicio + 12 meses) y estado de renovación automática
3. WHEN se confirma el pago de la primera cuota, THE License_Service SHALL transicionar el contrato de CREATED a ACTIVE y habilitar el acceso al módulo en menos de 60 segundos
4. THE License_Service SHALL mantener la cuota mensual fija e inmutable durante los 12 meses del contrato
5. IF se intenta crear un contrato ACTIVE para un módulo que ya tiene otro contrato ACTIVE en el mismo Tenant, THEN THE License_Service SHALL rechazar la operación con código HTTP 409
6. WHEN se solicita un upgrade (agregar módulo), THE License_Service SHALL crear un nuevo Contrato Anual independiente con su propio ciclo de 12 cuotas, sin modificar los contratos existentes
7. WHEN se solicita un downgrade, THE License_Service SHALL marcarlo como no renovable al vencimiento sin afectar el contrato vigente ni generar reembolso
8. IF se intenta solicitar la baja de un contrato mientras existen cuotas vencidas pendientes, THEN THE License_Service SHALL rechazar la operación con código HTTP 422

---

### Requerimiento 3: Política de Descuentos por Puntualidad de Pago

**User Story:** Como sistema de licencias, quiero aplicar automáticamente descuentos basados en la fecha de pago relativa a la Fecha Límite, para incentivar el pago anticipado y gestionar la suspensión por mora de forma consistente.

#### Criterios de Aceptación

1. WHEN el pago de una cuota se registra en cualquier día anterior a la Fecha_Limite, THE License_Service SHALL aplicar un descuento del 10% sobre el monto original y registrar el monto cobrado con descuento
2. WHEN el pago de una cuota se registra el mismo día de la Fecha_Limite, THE License_Service SHALL aplicar un descuento del 3% sobre el monto original
3. WHEN el pago de una cuota se registra exactamente 1 día después de la Fecha_Limite (día de gracia), THE License_Service SHALL cobrar el monto íntegro sin descuento ni penalización
4. WHEN una cuota llega al día +2 sin pago registrado, THE License_Service SHALL suspender el servicio del Tenant y generar automáticamente una Cuota_de_Almacenamiento
5. THE License_Service SHALL calcular la Cuota_de_Almacenamiento con la fórmula: 50 + CEIL(MAX(reportes_almacenados - 100, 0) / 100) × 10 euros por mes
6. WHILE un Tenant permanece en estado SUSPENDED, THE License_Service SHALL generar una nueva Cuota_de_Almacenamiento el primer día de cada mes de suspensión activa
7. THE License_Service SHALL requerir el pago total (100% del adeudo: cuotas vencidas + almacenamiento acumulado) para la reactivación; no se acepta pago parcial

---

### Requerimiento 4: Renovación Anual de Contratos

**User Story:** Como Cliente, quiero que mis contratos se renueven automáticamente con notificaciones previas adecuadas, para no perder el acceso por descuido y tener opción de no renovar si lo decido.

#### Criterios de Aceptación

1. WHEN un contrato anual está a 30 días de su Fecha de Vencimiento, THE License_Service SHALL iniciar la Ventana_de_Renovacion y publicar la primera notificación de renovación
2. WHILE el contrato está dentro de la Ventana_de_Renovacion, THE License_Service SHALL enviar notificaciones adicionales en los días -7 y -1 con: cuota mensual del nuevo período, tarifa de renovación (99 €) y opción de no renovar
3. WHEN la Fecha de Vencimiento se alcanza y la renovación automática está activa, THE License_Service SHALL cobrar la tarifa de renovación de 99 € más la primera cuota mensual del nuevo período anual
4. THE License_Service SHALL aplicar la tarifa de 99 € por cada contrato renovado individualmente; no se consolida si varios contratos vencen el mismo día
5. WHEN el Cliente desactiva la renovación automática con al menos 24 horas de anticipación, THE License_Service SHALL respetar la decisión y no ejecutar el cobro automático
6. IF el cobro de renovación falla en la Fecha de Vencimiento, THEN THE License_Service SHALL suspender el acceso al día siguiente (día +1 post-vencimiento) y generar Cuota_de_Almacenamiento
7. WHEN un contrato vence sin renovar, THE License_Service SHALL mantener el acceso activo hasta la Fecha de Vencimiento y desactivarlo automáticamente al día siguiente

---

### Requerimiento 5: Sistema de Créditos con Perfiles de Documento

**User Story:** Como sistema de licencias, quiero gestionar el consumo de créditos basado en el Perfil de Documento seleccionado (Asistencia Digital = 1.0, Asistente Automático de Normas = 1.5), para garantizar el descuento preciso y atómico del saldo del Tenant.

#### Criterios de Aceptación

1. WHEN un Técnico de campo envía una forma y el sistema genera exitosamente el PDF con perfil ASISTENCIA_DIGITAL, THE License_Service SHALL descontar exactamente 1.0 crédito del saldo del Tenant
2. WHEN un Técnico de campo envía una forma y el sistema genera exitosamente el PDF con perfil ASISTENTE_AUTOMATICO_NORMAS, THE License_Service SHALL descontar exactamente 1.5 créditos del saldo del Tenant
3. THE License_Service SHALL reservar el costo exacto del perfil antes de iniciar la generación del PDF usando una transacción con SELECT FOR UPDATE (nivel SERIALIZABLE); si la generación falla, el importe reservado se reintegra en 30 segundos o menos
4. IF el saldo de créditos del Tenant es inferior al costo del Perfil de Documento solicitado, THEN THE License_Service SHALL rechazar la operación con código HTTP 402 indicando saldo actual y costo requerido
5. THE License_Service SHALL almacenar el saldo de créditos con 2 decimales de precisión usando NUMERIC(10,2) en PostgreSQL y BigDecimal en Java
6. WHEN se confirma la compra de un paquete de créditos anuales, THE License_Service SHALL activar los créditos del paquete más 4 créditos bonus automáticos en el saldo del Tenant en el mismo instante
7. THE License_Service SHALL registrar en cada Evento de Crédito: perfil_documento, costo_creditos_aplicado, saldo_antes, saldo_despues, tecnico_id, documento_id y timestamp
8. WHEN el período anual de un paquete de créditos vence, THE License_Service SHALL caducar los créditos no consumidos (saldo a 0) sin transferirlos al siguiente período
9. THE License_Service SHALL mantener una tabla de costos por Perfil de Documento configurable por el Admin CRM

---

### Requerimiento 6: Contadores de Consultas y Excedente

**User Story:** Como sistema de licencias, quiero contabilizar todas las consultas a documentos existentes (visualización, firma, descarga, reenvío) y cobrar 1 crédito adicional por cada 100 consultas que excedan el umbral incluido, para facturar correctamente el uso por volumen.

#### Criterios de Aceptación

1. THE License_Service SHALL mantener un contador global de consultas por Tenant que acumule todas las consultas de todos los tipos de reporte durante el año en curso
2. THE License_Service SHALL mantener un contador individual por tipo de reporte por Tenant para análisis de uso
3. WHEN ocurre una visualización, firma electrónica, descarga de PDF, reenvío o consulta de estado sobre un documento existente, THE License_Service SHALL incrementar en +1 el contador global y el contador del tipo de reporte correspondiente
4. THE License_Service SHALL calcular el umbral de consultas incluidas como: créditos_totales_adquiridos_en_el_año × 100
5. WHEN el contador global supera el umbral y la consulta completa un bloque de 100 consultas de excedente, THE License_Service SHALL descontar 1 crédito del saldo del Tenant y publicar el evento `consulta.excedente`
6. THE License_Service SHALL cobrar excedente solo en bloques completos de 100 consultas; las fracciones dentro de un bloque no generan cobro
7. IF el saldo del Tenant es 0 cuando ocurre un cobro por excedente, THEN THE License_Service SHALL registrar la deuda y notificar al administrador sin bloquear las consultas
8. WHEN se inicia un nuevo período anual del contrato, THE License_Service SHALL reiniciar los contadores de consultas a 0
9. THE License_Service SHALL recalcular automáticamente el umbral cuando el Tenant adquiere créditos adicionales (top-up): nuevo umbral = (créditos originales + top-up) × 100

---

### Requerimiento 7: Apertura de Contrato (Onboarding)

**User Story:** Como Cliente nuevo del Mikel CRM, quiero completar el pago de Apertura de Contrato eligiendo la modalidad que mejor se adapta a mi empresa (Estándar 400 € / Personalizada 1.400 €), para acceder al CRM con los créditos de bienvenida incluidos.

#### Criterios de Aceptación

1. THE License_Service SHALL requerir el pago de Apertura de Contrato como paso obligatorio previo al acceso al CRM para todo Tenant nuevo
2. WHEN se selecciona la Apertura Estándar (400 €), THE License_Service SHALL activar 2 créditos de bienvenida, establecer la modalidad de reporte como ESTANDAR y completar la activación de forma automatizada
3. WHEN se selecciona la Apertura Personalizada (1.400 €), THE License_Service SHALL activar 10 créditos de bienvenida, establecer la modalidad de reporte como PERSONALIZADO e iniciar el flujo de onboarding asistido
4. THE License_Service SHALL registrar la Apertura de Contrato como un pago único no recurrente separado del ciclo de cuotas mensuales
5. THE License_Service SHALL garantizar que los créditos de bienvenida no caduquen antes de los primeros 12 meses de contrato activo
6. WHEN un Cliente con Apertura Estándar solicita upgrade a Personalizada, THE License_Service SHALL procesar el cobro de 1.000 € (diferencia) y activar 8 créditos adicionales sin interrumpir los contratos vigentes

---

### Requerimiento 8: Validación de Acceso (Query API — Hot Path)

**User Story:** Como SaaS Core del Mikel CRM, quiero validar el acceso de cada usuario en cada acción mediante una consulta ultrarrápida al License Service, para no degradar la experiencia del usuario con latencia perceptible.

#### Criterios de Aceptación

1. WHEN el SaaS Core solicita validación de acceso para un Tenant, THE Query_API SHALL responder con el estado del Tenant, módulos activos, saldo de créditos y rol del usuario en menos de 5 ms (p99) cuando hay cache hit en Redis
2. WHILE la clave `tenant:{tenantId}:access` existe en Redis_Cache con TTL válido, THE Query_API SHALL servir la respuesta directamente desde Redis sin consultar PostgreSQL
3. IF la clave Redis no existe o ha expirado (TTL 300 segundos), THEN THE Query_API SHALL consultar PostgreSQL, escribir la respuesta en Redis y responder al cliente en menos de 30 ms
4. WHEN cualquier Command modifica el estado de un Tenant (suspensión, reactivación, cambio de módulos, consumo de créditos), THE License_Service SHALL invalidar inmediatamente la clave Redis del Tenant afectado
5. IF un Tenant está en estado SUSPENDED, THEN THE Query_API SHALL responder con status SUSPENDED, fecha de suspensión, adeudo total y URL de resumen de reactivación
6. THE Query_API SHALL soportar 500 req/s por instancia en pico con una tasa de cache hit de al menos 95%

---

### Requerimiento 9: Command API (Escritura)

**User Story:** Como panel de administración del Mikel CRM, quiero ejecutar operaciones de escritura (crear contratos, registrar pagos, consumir créditos, reactivar tenants) mediante comandos asíncronos, para mantener la consistencia del sistema y la trazabilidad de cada operación.

#### Criterios de Aceptación

1. THE Command_API SHALL procesar todos los Commands con latencia menor a 200 ms (p99) y responder con HTTP 202 Accepted más un correlationId
2. WHEN se registra un pago de cuota mensual, THE Command_API SHALL calcular y aplicar el descuento correcto (10%, 3% o 0%) basándose en la fecha de pago relativa a la Fecha_Limite
3. WHEN se procesa una renovación anual, THE Command_API SHALL cobrar la tarifa fija de 99 € más la primera cuota del nuevo período y crear un nuevo Contrato_Anual con fechas de inicio +12 meses
4. WHEN se solicita la reactivación de un Tenant suspendido, THE Command_API SHALL validar que el monto proporcionado es exactamente igual al adeudo total (cuotas vencidas + almacenamiento) antes de procesar el pago
5. WHEN se ejecuta un consumo de créditos, THE Command_API SHALL usar una transacción SERIALIZABLE con SELECT FOR UPDATE para garantizar atomicidad y evitar condiciones de carrera
6. THE Command_API SHALL publicar un Domain_Event por cada transición de estado exitosa al topic Kafka `license-events`

---

### Requerimiento 10: Domain Events

**User Story:** Como ecosistema de microservicios del Mikel CRM, quiero recibir eventos de dominio inmutables del License Service para cada cambio de estado relevante, para reaccionar de forma desacoplada (notificaciones, facturación, analytics).

#### Criterios de Aceptación

1. THE License_Service SHALL publicar los siguientes eventos al topic Kafka `license-events`: tenant.onboarded, tenant.activated, tenant.suspended, tenant.reactivated, tenant.cancelled, contrato.created, contrato.renewed, contrato.expired, cuota.charged, cuota.overdue, credito.consumed, credito.bonus_granted, credito.balance_low, consulta.excedente, storage.fee_generated, access.denied
2. THE License_Service SHALL serializar cada evento con el schema base: eventId (UUID v4), eventType, version, tenantId, occurredAt (ISO8601), payload y metadata (correlationId, causationId, service, environment)
3. WHEN un evento se publica exitosamente, THE License_Service SHALL garantizar que sea inmutable y versionado para soporte de evolución de schema
4. THE License_Service SHALL publicar `credito.balance_low` cuando el saldo del Tenant alcance los umbrales del 20%, 10% y 0% del total adquirido

---

### Requerimiento 11: Jobs Programados (Cron)

**User Story:** Como sistema de licencias, quiero ejecutar procesos programados para detectar cuotas vencidas, procesar renovaciones automáticas, generar cargos de almacenamiento, caducar créditos y cancelar tenants inactivos, para automatizar el cumplimiento de las reglas de negocio temporales.

#### Criterios de Aceptación

1. THE License_Service SHALL ejecutar el CuotaOverdueJob diariamente a la 1 AM para detectar cuotas con Fecha_Limite +2 sin pago y publicar `cuota.overdue` disparando la suspensión
2. THE License_Service SHALL ejecutar el RenovacionNotificacionJob diariamente a las 8 AM para enviar alertas de renovación a contratos en los días -30, -7 y -1 de su vencimiento
3. THE License_Service SHALL ejecutar el RenovacionAutoJob diariamente a las 9 AM para procesar la renovación automática de contratos que vencen ese día con renovacion_auto = true
4. THE License_Service SHALL ejecutar el CuotaAlmacenamientoJob el día 1 de cada mes a las 2 AM para generar Cuotas de Almacenamiento para todos los Tenants en estado SUSPENDED
5. THE License_Service SHALL ejecutar el CreditosVencimientoJob diariamente a las 3 AM para caducar créditos de paquetes cuya fecha de vencimiento sea el día anterior
6. THE License_Service SHALL ejecutar el TenantCancelacionJob diariamente a las 4 AM para cancelar automáticamente Tenants que llevan 90 o más días en estado SUSPENDED sin pago

---

### Requerimiento 12: RBAC (Control de Acceso Basado en Roles)

**User Story:** Como plataforma Mikel CRM, quiero que el License Service valide los permisos de cada usuario según su rol asignado dentro del Tenant, para garantizar que solo acceden a las funcionalidades autorizadas.

#### Criterios de Aceptación

1. THE License_Service SHALL soportar los roles: ADMIN_CUENTA (gestión de contratos, usuarios, licencias, créditos dentro de su Tenant), SUPERVISOR (tablero, tickets, aprobación, eFirma), TECNICO (enviar formas, generar PDF, consultar documentos propios) y ASISTENTE (leer/crear/actualizar clientes y tickets, consultar reportes)
2. WHEN un usuario solicita acceso a un recurso, THE License_Service SHALL evaluar el permiso del rol del usuario contra la matriz de permisos (recurso, acción) antes de permitir la operación
3. IF el rol del usuario no tiene el permiso requerido para la acción solicitada, THEN THE License_Service SHALL responder con HTTP 403 y registrar el intento denegado en el Audit_Log
4. THE License_Service SHALL validar en cada request que el Tenant del token JWT coincide con el Tenant del recurso solicitado; un acceso cross-tenant se deniega con HTTP 403
5. WHILE el módulo asociado al recurso esté suspendido o sin licencia activa, THE License_Service SHALL denegar el acceso independientemente de los permisos del rol
6. THE License_Service SHALL resolver la autorización en menos de 5 ms adicionales (p99) usando caché de la matriz de permisos por rol en Redis

---

### Requerimiento 13: Audit Log Inmutable

**User Story:** Como plataforma Mikel CRM, quiero registrar cada acción y transición de estado en un log inmutable particionado, para cumplir con los requisitos de auditoría y trazabilidad del sistema.

#### Criterios de Aceptación

1. THE License_Service SHALL registrar en el Audit_Log cada Command ejecutado con: tenant_id, usuario_id, acción, entidad, entidad_id, payload_antes, payload_despues, resultado (OK, DENEGADO, ERROR), ip_origen y timestamp
2. THE Audit_Log SHALL ser append-only: no se permiten operaciones UPDATE ni DELETE sobre la tabla
3. THE License_Service SHALL particionar la tabla de Audit_Log por mes para mantener el rendimiento de escritura y facilitar la retención
4. WHEN un acceso es denegado por RBAC o por estado del Tenant, THE License_Service SHALL registrar el evento con resultado DENEGADO incluyendo el motivo específico de la denegación
5. THE License_Service SHALL registrar los accesos denegados con la misma estructura que las acciones exitosas para análisis de seguridad

---

### Requerimiento 14: Modelo de Datos y Persistencia

**User Story:** Como equipo de desarrollo, quiero un modelo de datos relacional completo con restricciones de integridad, índices optimizados y migraciones versionadas, para garantizar la consistencia y el rendimiento del sistema.

#### Criterios de Aceptación

1. THE License_Service SHALL implementar las entidades: Tenant, Contrato_Anual, Cuota_Mensual, Renovacion, Cuota_Almacenamiento, Paquete_Creditos, Evento_Credito, Contador_Consultas, Rol_Usuario y Audit_Log con las relaciones definidas en el modelo ER
2. THE License_Service SHALL aplicar la restricción CHECK (saldo_disponible >= 0) en la tabla paquete_creditos para evitar saldos negativos
3. THE License_Service SHALL usar migraciones Flyway versionadas para toda evolución del esquema de base de datos
4. THE License_Service SHALL crear índices específicos para: validación de acceso (tenant_id + estado en contrato_anual), cuotas vencidas (fecha_limite + estado en cuota_mensual), renovaciones próximas (fecha_vencimiento + renovacion_auto) y contadores (tenant_id + periodo_año)
5. THE License_Service SHALL usar `NUMERIC(10,2)` para todos los campos monetarios y de créditos; nunca tipos float o double

---

### Requerimiento 15: Seguridad y Rendimiento

**User Story:** Como equipo de operaciones, quiero que el License Service cumpla con estándares de seguridad (autenticación JWT, confinamiento de Tenant, rate limiting) y rendimiento (latencia objetivo, escalabilidad horizontal), para operar de forma confiable en producción.

#### Criterios de Aceptación

1. THE License_Service SHALL autenticar todas las requests mediante JWT firmado con RS256 por el auth-service, con TTL de 15 minutos
2. THE License_Service SHALL validar el claim `tenantId` del JWT contra el recurso solicitado en cada request para garantizar confinamiento de Tenant
3. THE License_Service SHALL aplicar rate limiting de 1000 req/s por Tenant en Query API y 100 req/s por Tenant en Command API usando un contador de ventana deslizante en Redis
4. THE License_Service SHALL nunca almacenar datos sensibles de pago (números de tarjeta, CVV); solo la referencia externa del Payment Service
5. THE License_Service SHALL mantener disponibilidad del 99.9% mensual (menos de 44 minutos de downtime al mes)
6. THE License_Service SHALL procesar los Cron Jobs de cuotas vencidas en menos de 5 minutos para hasta 10.000 contratos, usando batch de 500 con procesamiento asíncrono

