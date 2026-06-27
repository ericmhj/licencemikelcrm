# Requerimientos e Historias de Usuario
## Sistema de Licencias — Mikel CRM

**Versión:** 1.3 — Draft  
**Fecha:** 2026-06-20  
**Autor:** Analista de Sistemas — LicenciasCRMMikel  
**Estado:** Draft  
**Cambios v1.1:** Incorporación del modelo de Apertura de Contrato (Estándar y Personalizado) con créditos incluidos.  
**Cambios v1.2:** Épica 10 — Portal de Contratación (US-40/US-41/US-42): pantalla de prospecto con calculadora, dashboard de tenant autenticado y configuración de umbrales y promociones (RN-UMP-01..05).  
**Cambios v1.3:** Definición de Perfiles de Documento con costo diferenciado en créditos (Asistencia Digital = 1 crédito; Asistente Automático de Normas = 1.5 créditos). Incorporación del actor Técnico de campo. Nuevos RF-07.17/18/19 y reglas RN-C-PDF-01..04. ⚠ Denominación anterior "Asistente de Cumplimiento de Normas" → reemplazada en todo el sistema por **Asistente Automático de Normas**.

---

## Tabla de Contenidos

1. [Glosario](#glosario)
2. [Actores del Sistema](#actores-del-sistema)
3. [Requerimientos Funcionales](#requerimientos-funcionales)
4. [Requerimientos No Funcionales](#requerimientos-no-funcionales)
5. [Épicas e Historias de Usuario](#épicas-e-historias-de-usuario)
6. [Matriz de Trazabilidad](#matriz-de-trazabilidad)

---

## Glosario

| Término | Definición |
|---------|-----------|
| **Licencia** | Contrato digital que habilita el acceso a uno o más módulos del Mikel CRM por un período determinado |
| **Módulo** | Unidad funcional del CRM que puede activarse o desactivarse independientemente (ej: Ventas, Soporte, Reportes) |
| **Suscripción** | Modalidad de pago recurrente mensual o anual que mantiene activa la licencia |
| **Licencia por Créditos** | Modalidad de licencia anual en la que el Cliente adquiere un paquete de créditos; cada crédito equivale a una utilización de una funcionalidad o acción específica del CRM |
| **Crédito** | Unidad de consumo del sistema. El costo en créditos varía según el **Perfil de Documento** generado por el Técnico de campo: `Asistencia Digital = 1.0 crédito`; `Asistente Automático de Normas = 1.5 créditos`. El saldo se almacena con 2 decimales para soportar consumos fraccionados. Ningún evento posterior a la generación del PDF (firma, descarga, visualización) consume crédito adicional |
| **Evento de Crédito** | El único evento que descuenta del saldo: envío de forma por un Técnico de campo + generación exitosa del PDF. Es atómico: si el PDF falla, el crédito reservado se reintegra. El importe descontado depende del Perfil de Documento activo |
| **Perfil de Documento** | Categoría del documento que determina el costo en créditos al generar el PDF. El sistema soporta actualmente dos perfiles: **Asistencia Digital** (1.0 crédito) y **Asistente Automático de Normas** (1.5 créditos). Cada tenant contrata el perfil habilitado en su plan |
| **Asistencia Digital** | Perfil de documento para servicios de asistencia técnica in situ. La generación de un PDF bajo este perfil consume **1.0 crédito** del saldo del tenant. Ejecutado por el Técnico de campo |
| **Asistente Automático de Normas** | Perfil de documento para verificación y cumplimiento normativo automatizado. La generación de un PDF bajo este perfil consume **1.5 créditos** del saldo del tenant. Ejecutado por el Técnico de campo. ⚠ Denominación anterior: *Asistente de Cumplimiento de Normas* — cualquier referencia a este nombre debe entenderse como Asistente Automático de Normas |
| **Consulta** | Cualquier acceso posterior al documento ya creado: visualización, firma electrónica, descarga del PDF, reenvío, o cualquier paso del flujo documental posterior al evento de crédito. Las consultas **no consumen crédito** pero **sí se contabilizan** |
| **Umbral de Consultas Incluidas** | Número de consultas gratuitas por tenant = `créditos_totales_adquiridos × 100`. Estimación base: un documento normal genera ≤ 100 consultas durante su vida útil anual |
| **Excedente de Consultas** | Consultas que superan el Umbral de Consultas Incluidas del tenant. Se cobra **1 crédito adicional por cada 100 consultas de excedente**, contabilizando todas las consultas de todos los reportes del tenant de forma agregada |
| **Contador por Tipo de Reporte** | Acumulador individual por tenant que registra el total de consultas para un tipo de reporte específico. Permite análisis de uso por categoría |
| **Contador Global del Tenant** | Acumulador maestro por tenant que suma las consultas de todos los tipos de reporte. Es el valor que se compara contra el Umbral de Consultas Incluidas para determinar excedente |
| **Tenant** | Organización/empresa cliente que opera en una instancia lógicamente aislada del Mikel CRM. Todos los contadores y el saldo de créditos se gestionan a nivel de tenant |
| **Paquete de Créditos** | Conjunto de créditos adquiridos de una sola vez con vigencia de 12 meses desde la fecha de activación |
| **Saldo de Créditos** | Cantidad de créditos disponibles (no consumidos) en un momento dado |
| **Utilización** | Sinónimo de Evento de Crédito: envío de forma + generación de PDF |
| **Cliente / Empresa** | Organización que adquiere licencias del Mikel CRM |
| **Ciclo de facturación** | Período de cobro mensual de una cuota dentro de un contrato anual. Todos los contratos del sistema son anuales; el pago se fracciona en 12 cuotas mensuales |
| **Contrato Anual** | Único modelo de contratación del Mikel CRM. El Cliente se compromete a 12 meses de permanencia y paga una cuota mensual cada mes. Aplica a todos los módulos y paquetes de créditos |
| **Cuota Mensual** | Pago periódico equivalente a 1/12 del valor anual del contrato. Se cobra en la misma fecha de aniversario cada mes durante los 12 meses del contrato |
| **Fecha de Aniversario** | Día del mes en que se cobra la cuota mensual, igual al día de inicio del contrato (ej: si el contrato inicia el día 5, la cuota se cobra el día 5 de cada mes) |
| **Fecha de Vencimiento del Contrato** | Fecha en que el contrato anual completa sus 12 meses. A partir de esta fecha el sistema inicia el proceso de renovación automática |
| **Ventana de Renovación** | Período de 30 días previos a la Fecha de Vencimiento del Contrato durante el cual el sistema notifica al Cliente y gestiona la renovación |
| **Activación** | Proceso que habilita el acceso a un módulo tras confirmar el pago de la primera cuota mensual |
| **Suspensión** | Estado en que el acceso operativo se bloquea: (a) por cuota mensual no pagada a partir del día +2 del vencimiento, o (b) por no renovación al finalizar el contrato anual |
| **Vencimiento** | Fecha en que una cuota mensual o el contrato anual dejan de ser válidos si no se pagan o renuevan |
| **Downgrade** | Reducción de módulos, aplicable únicamente al momento de la renovación anual del contrato |
| **Upgrade** | Adición de módulos, efectivo de forma inmediata con cuota prorrateada al mes en curso |
| **Permanencia** | Período mínimo de 12 meses al que se compromete el Cliente al firmar cualquier contrato anual del sistema |
| **Módulo Administrador** | Módulo del Mikel CRM orientado al Supervisor o Coordinador. Incluye tablero en tiempo real del equipo, gestión de tickets solicitud→entrega, aprobación y eFirma de supervisor, e historial completo para auditorías |
| **Ticket** | Unidad de trabajo gestionada dentro del Módulo Administrador que representa una solicitud de servicio desde su apertura hasta su entrega |
| **eFirma de Supervisor** | Firma electrónica emitida por el rol Supervisor/Coordinador para aprobar documentos dentro del flujo del ticket |
| **Apertura de Contrato** | Pago único obligatorio que el Cliente realiza al inicio de la relación comercial. Activa el CRM y determina la modalidad de reporte (Estándar o Personalizado). Incluye créditos iniciales |
| **Apertura Estándar** | Modalidad de apertura de contrato por 400 € que activa el reporte predefinido del sistema sin personalización de marca. Incluye 2 créditos |
| **Apertura Personalizada** | Modalidad de apertura de contrato por 1.400 € que incluye personalización completa del reporte (logotipo, cabecera, pie de página y secciones a medida). Incluye 10 créditos |
| **Reporte Estándar** | Reporte generado por el sistema con plantilla predefinida de Mikel CRM, sin elementos de marca del Cliente |
| **Reporte Personalizado** | Reporte generado con la identidad visual del Cliente (logotipo, colores, cabecera, pie de página) y secciones configuradas según sus necesidades específicas |
| **Créditos de Bienvenida** | Créditos incluidos en la Apertura de Contrato, disponibles desde el primer día. No caducan antes del primer año completo de contrato |
| **Renovación de Contrato Anual** | Proceso automático que extiende cualquier contrato anual del sistema (módulos, paquetes de créditos) por un nuevo período de 12 meses al costo de **99 €**. El cobro se realiza en la fecha de vencimiento del contrato vigente |
| **Créditos Bonus de Paquete** | 4 créditos adicionales entregados automáticamente al tenant al adquirir o renovar cualquier paquete de créditos anuales. Se suman al saldo general y heredan la fecha de vencimiento del paquete adquirido |
| **Fecha Límite de Pago** | Fecha de vencimiento de una factura o cuota. Es el día 0 de la ventana de pago |
| **Pago Anticipado** | Pago realizado en cualquier día anterior a la Fecha Límite. Aplica un descuento del **10 %** sobre el monto de la factura |
| **Pago en Fecha Límite** | Pago realizado el mismo día del vencimiento (día 0). Aplica un descuento del **3 %** sobre el monto de la factura |
| **Pago en Gracia** | Pago realizado exactamente 1 día después de la Fecha Límite (día +1). Se cobra el monto normal sin descuento ni penalización |
| **Pago Vencido** | Pago no recibido a partir del día +2 posterior a la Fecha Límite. Desencadena la suspensión del servicio y la generación de una Cuota de Almacenamiento |
| **Cuota de Almacenamiento** | Cargo mensual que se factura al tenant mientras su servicio esté suspendido por Pago Vencido. Base: **50 €/mes** para hasta 100 reportes almacenados; se incrementa **10 € por cada 100 reportes adicionales** (o fracción) por encima de los primeros 100 |
| **Reactivación por Mora** | Proceso de restauración del servicio tras una suspensión por Pago Vencido. Requiere el pago total de: todas las cuotas vencidas pendientes + todas las Cuotas de Almacenamiento acumuladas durante el período de suspensión |

---

## Referencia de Arquitectura

> Este documento define los **requerimientos funcionales y las historias de usuario** del sistema de licencias.
> La especificación técnica completa (arquitectura CQRS, modelo de datos, API, jobs, eventos de dominio y reglas de negocio implementables) se encuentra en:
>
> 📄 **`SDD_MICROSERVICIO_LICENCIAS.md`** — Microservicio `license-service` (Spring Boot · PostgreSQL · Redis)

---

## Actores del Sistema

| Actor | Descripción | Permisos principales |
|-------|-------------|---------------------|
| **Cliente / Empresa** | Administrador de la cuenta del CRM que gestiona su propia suscripción | Ver estado de licencia, activar/desactivar módulos, renovar, actualizar datos de pago |
| **Técnico de campo** | Usuario operativo (rol `TECNICO`) que ejecuta servicios en campo y genera documentos PDF usando los perfiles habilitados para su tenant. Es el actor que dispara el Evento de Crédito | Enviar formas, generar PDFs (Asistencia Digital o Asistente Automático de Normas), consultar documentos propios |
| **Sistema (automatizado)** | Procesos internos de verificación, cobro, expiración y notificación | Ejecutar transiciones de estado, enviar notificaciones, procesar cobros |
| **Admin CRM (interno)** | Operador interno de Mikel para soporte y gestión de cuentas | Gestión total sobre cualquier licencia |

> **Nota:** Las historias de usuario de este documento se centran en el actor **Cliente / Empresa**, con participación implícita del Sistema para flujos automáticos.

---

## Requerimientos Funcionales

### RF-01 — Gestión de Licencias por Módulo

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-01.1 | El sistema debe permitir al Cliente activar uno o más módulos del CRM de forma independiente | Alta |
| RF-01.2 | El sistema debe mostrar al Cliente el catálogo de módulos disponibles con descripción, precio mensual y precio anual | Alta |
| RF-01.3 | El sistema debe permitir agregar módulos adicionales a una suscripción activa en cualquier momento | Alta |
| RF-01.4 | El sistema debe permitir desactivar un módulo, con efecto al final del ciclo de facturación actual | Media |
| RF-01.5 | El sistema debe restringir el acceso a las funcionalidades de un módulo cuando su licencia no está activa | Alta |
| RF-01.6 | El sistema debe mostrar un aviso dentro del CRM cuando el Cliente intenta acceder a un módulo no licenciado, con opción de activarlo | Media |

---

### RF-02 — Modelo de Contrato Anual con Cuotas Mensuales (Universal)

> **Principio rector:** Todos los contratos del Mikel CRM son anuales. No existe modalidad mensual renovable. El Cliente se compromete a 12 meses de permanencia y paga una cuota mensual durante ese período. Este modelo aplica a todos los módulos y paquetes de créditos sin excepción.

#### Ciclo de vida de un contrato anual

```
Contratación → [Pago cuota 1] → Acceso activo
    → [Cuota 2 … 11 mensuales]
    → Ventana de Renovación (día -30 al día 0 del vencimiento)
        → [Cliente renueva] → Nuevo contrato anual (tarifa 99 € + nueva cuota mensual)
        → [Cliente no renueva] → Acceso activo hasta día 0 → Suspensión día +1
    → Cuota vencida (día +2 sin pago) → Suspensión + Cuota de Almacenamiento
```

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-02.1 | El único modelo de contratación disponible es el **Contrato Anual con Cuotas Mensuales**: compromiso de 12 meses, pago fraccionado en 12 cuotas mensuales iguales | Alta |
| RF-02.2 | El sistema debe mostrar al contratar: cuota mensual, duración del compromiso (12 meses), coste total anual, fecha de inicio y fecha de vencimiento del contrato | Alta |
| RF-02.3 | El sistema debe cobrar la primera cuota al confirmar la contratación; las cuotas 2 a 12 se cobran el mismo día de aniversario de cada mes | Alta |
| RF-02.4 | El sistema debe iniciar la **Ventana de Renovación** 30 días antes de la Fecha de Vencimiento del Contrato, enviando la primera notificación de renovación en ese momento | Alta |
| RF-02.5 | Durante la Ventana de Renovación (día -30 a día 0), el sistema debe enviar notificaciones adicionales en los días -7 y -1, indicando en cada una: cuota mensual del nuevo período, tarifa de renovación (99 €) y opción de no renovar | Alta |
| RF-02.6 | El sistema debe renovar automáticamente el contrato al vencimiento cobrando la **tarifa de renovación de 99 €** más la primera cuota mensual del nuevo período anual, salvo que el Cliente haya desactivado la renovación automática | Alta |
| RF-02.7 | El sistema debe permitir al Cliente desactivar la renovación automática con al menos 24 horas de anticipación a la Fecha de Vencimiento del Contrato | Alta |
| RF-02.8 | Si el Cliente no renueva, el acceso al módulo o paquete se mantiene activo hasta la Fecha de Vencimiento y se desactiva automáticamente al día siguiente | Alta |
| RF-02.9 | El sistema debe mostrar en el panel el estado de cada contrato anual: cuotas pagadas (N/12), cuotas pendientes, próxima fecha de cobro, fecha de vencimiento del contrato y estado de la renovación automática | Alta |
| RF-02.10 | El upgrade (agregar módulos) genera un nuevo contrato anual independiente con su propio ciclo de cuotas mensuales y fecha de vencimiento; no modifica los contratos existentes | Alta |
| RF-02.11 | El downgrade (quitar módulos) solo es efectivo al finalizar el contrato anual vigente del módulo en cuestión; no se puede aplicar en mitad del período contratado | Alta |

---

### RF-03 — Activación y Onboarding de Licencia

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-03.1 | El sistema debe permitir iniciar la activación de una licencia desde el panel del CRM sin salir de la aplicación | Alta |
| RF-03.2 | El sistema debe confirmar la activación enviando un correo electrónico al contacto principal del Cliente | Alta |
| RF-03.3 | El sistema debe habilitar el acceso al módulo en un tiempo máximo de 60 segundos tras la confirmación de pago | Alta |
| RF-03.4 | El sistema debe ofrecer un período de prueba (trial) de 14 días para módulos nunca antes contratados, sin requerir datos de pago | Media |
| RF-03.5 | El sistema debe limitar el trial a una sola vez por módulo por empresa | Alta |

---

### RF-04 — Gestión de Cuotas Mensuales, Mora y Renovación Anual

> Esta sección cubre dos flujos de pago distintos: (A) las **cuotas mensuales** del contrato anual en curso, y (B) la **renovación** al término de los 12 meses.

#### A — Gestión de cuotas mensuales (meses 1 a 12)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-04.1 | El sistema debe notificar al Cliente por correo 3 días antes de cada Fecha de Aniversario (vencimiento de cuota mensual), indicando el monto a cobrar y el descuento disponible si paga antes de la fecha límite | Alta |
| RF-04.2 | El sistema debe permitir al Cliente actualizar el método de pago en cualquier momento desde su panel | Alta |
| RF-04.3 | El sistema debe reintentar el cobro automático de una cuota mensual hasta 2 veces adicionales: el día 0 (Fecha de Aniversario) y el día +1 (día de gracia), aplicando el descuento correspondiente a cada intento según la política de RF-14 | Alta |
| RF-04.4 | Si el cobro no se realiza exitosamente antes del día +2, el sistema suspende el acceso operativo del módulo o paquete afectado y genera la Cuota de Almacenamiento conforme a RF-14.4 y RF-14.5 | Alta |
| RF-04.5 | La suspensión por cuota mensual impagada **no cancela el contrato anual**; el Cliente sigue obligado a las cuotas pendientes y debe pagar la deuda acumulada (cuotas + almacenamiento) para reactivar | Alta |
| RF-04.6 | El sistema debe restaurar el acceso en menos de 60 segundos tras recibir el pago completo de reactivación (cuotas vencidas + Cuotas de Almacenamiento acumuladas) | Alta |
| RF-04.7 | El sistema debe emitir y enviar una factura por cada cuota mensual cobrada, reflejando el descuento aplicado cuando corresponda | Alta |

#### B — Renovación anual (mes 12 → nuevo período)

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-04.8 | El sistema debe iniciar la Ventana de Renovación en el día -30 de la Fecha de Vencimiento del Contrato, enviando la primera notificación con las opciones de renovar o no renovar | Alta |
| RF-04.9 | El sistema debe enviar recordatorios adicionales de renovación en los días -7 y -1, incluyendo en cada uno: cuota mensual del nuevo período, tarifa de renovación (99 €), total del primer cobro de renovación y fecha límite para desactivar la renovación automática | Alta |
| RF-04.10 | Si el Cliente no desactiva la renovación antes del día -1, el sistema cobra automáticamente en la Fecha de Vencimiento: la tarifa de renovación de 99 € + la primera cuota mensual del nuevo período anual | Alta |
| RF-04.11 | Si el cobro de renovación falla en la Fecha de Vencimiento, el sistema suspende el acceso al día siguiente (día +1 post-vencimiento) y genera la Cuota de Almacenamiento | Alta |
| RF-04.12 | El sistema debe emitir facturas separadas por: (a) la tarifa de renovación de 99 € y (b) cada cuota mensual del nuevo período | Alta |

---

### RF-05 — Visibilidad y Autogestión del Cliente

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-05.1 | El sistema debe mostrar un panel de licencias con el estado de cada módulo, fechas de vencimiento y próximo monto a cobrar | Alta |
| RF-05.2 | El sistema debe mostrar el historial completo de pagos y facturas, descargables en PDF | Media |
| RF-05.3 | El sistema debe mostrar un resumen del uso por módulo (ej: últimos 30 días de actividad) para apoyar decisiones de upgrade/downgrade | Baja |
| RF-05.4 | El sistema debe permitir al Cliente cancelar completamente su suscripción, con confirmación explícita y aviso sobre retención de datos | Alta |
| RF-05.5 | Tras la cancelación, el sistema debe retener los datos del Cliente durante 90 días antes de purgarlos | Alta |

---

### RF-06 — Reglas de Negocio del Contrato Anual

| ID | Regla | Prioridad |
|----|-------|-----------|
| RF-06.1 | Un módulo solo puede estar activo si tiene un Contrato Anual vigente con cuotas al corriente de pago o dentro del período de gracia (día +1) | Alta |
| RF-06.2 | El downgrade (quitar módulos) **solo puede solicitarse y aplicarse al finalizar el contrato anual vigente del módulo**; no es posible en ningún mes intermedio del período contratado ni genera reembolso | Alta |
| RF-06.3 | El upgrade (agregar módulos) genera un nuevo Contrato Anual independiente efectivo de forma inmediata; la primera cuota se prorratea por los días restantes del mes en curso | Alta |
| RF-06.4 | Una empresa no puede tener dos Contratos Anuales activos para el mismo módulo simultáneamente | Alta |
| RF-06.5 | El trial no genera contrato ni cuotas; al terminar el trial sin contratar, no hay cargo. Al contratar tras el trial, se formaliza un Contrato Anual completo desde ese día | Media |
| RF-06.6 | La cancelación definitiva del contrato no genera reembolso por las cuotas mensuales no consumidas dentro del período anual contratado | Media |
| RF-06.7 | La suspensión por cuota impagada no extingue el Contrato Anual; el compromiso de permanencia se mantiene y las cuotas pendientes siguen siendo exigibles | Alta |
| RF-06.8 | El Cliente no puede solicitar la baja de un módulo mientras tenga cuotas vencidas pendientes; primero debe regularizar el adeudo | Alta |

---

### RF-07 — Definición y Consumo de Créditos

> **Modelo preciso:** El Técnico de campo envía una forma y el sistema genera el PDF resultante. El costo en créditos depende del **Perfil de Documento** habilitado en el tenant: `Asistencia Digital = 1.0 crédito`; `Asistente Automático de Normas = 1.5 créditos`. Todo evento posterior al PDF (firmas, descargas, visualizaciones, reenvíos) **no consume crédito adicional** pero sí se contabiliza como "consulta" para el control de excedente por volumen.

#### Tabla de costos por Perfil de Documento

| Perfil de Documento | Actor | Costo en Créditos | Nota |
|---------------------|-------|-------------------|------|
| Asistencia Digital | Técnico de campo | **1.0 crédito** | Perfil estándar de asistencia técnica in situ |
| Asistente Automático de Normas | Técnico de campo | **1.5 créditos** | Antes denominado "Asistente de Cumplimiento de Normas" |

#### Diagrama del flujo de un documento y su impacto en créditos

```
Técnico de campo completa forma
        │
        ▼
[ENVÍA FORMA] ──► [Detecta Perfil de Documento]
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
   Asistencia Digital      Asistente Automático de Normas
   [GENERA PDF]            [GENERA PDF]
        │                       │
   ✦ -1.0 CRÉDITO ✦        ✦ -1.5 CRÉDITOS ✦
        │                       │
        └───────────┬───────────┘
                    ▼
       ┌────────────┼────────────┐
       ▼            ▼            ▼
 [Firma electr.]  [Descarga]  [Visualización]
                    │
         CONSULTA (+1 al contador)
         ✗ NO consume crédito adicional
```

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-07.1 | El sistema debe consumir del saldo del tenant el **costo definido por el Perfil de Documento** cuando un Técnico de campo envía una forma y el sistema genera exitosamente el PDF: 1.0 crédito para Asistencia Digital, 1.5 créditos para Asistente Automático de Normas | Alta |
| RF-07.2 | El sistema debe **reservar** el costo exacto del perfil antes de iniciar la generación del PDF para evitar condiciones de carrera; si la generación falla, el importe reservado se reintegra automáticamente en ≤ 30 segundos | Alta |
| RF-07.3 | Ningún evento posterior a la generación del PDF (firma electrónica, descarga, visualización, reenvío, consulta de estado) debe descontar créditos adicionales del saldo | Alta |
| RF-07.4 | El sistema debe permitir al Cliente adquirir paquetes de créditos anuales de diferentes tamaños (ej: 10, 50, 100, 500 créditos) | Alta |
| RF-07.5 | El sistema debe bloquear el envío de una forma cuando el saldo de créditos del tenant sea **inferior al costo del Perfil de Documento** solicitado (< 1.0 para Asistencia Digital; < 1.5 para Asistente Automático de Normas), mostrando un aviso con el saldo actual, el costo requerido y opción de comprar créditos antes de continuar | Alta |
| RF-07.6 | El sistema debe mostrar en tiempo real el saldo de créditos disponibles en el panel del CRM | Alta |
| RF-07.7 | El sistema debe notificar al tenant cuando su saldo de créditos llegue al 20%, 10% y 0% del total adquirido | Alta |
| RF-07.8 | El sistema debe permitir adquirir créditos adicionales (top-up) en cualquier momento dentro del período anual vigente | Alta |
| RF-07.9 | Los créditos de top-up heredan la fecha de vencimiento del paquete anual activo, no generan un nuevo período de 12 meses | Alta |
| RF-07.10 | Los créditos no consumidos al vencimiento del período anual caducan y no se transfieren al siguiente período | Alta |
| RF-07.11 | El sistema debe registrar cada evento de consumo de crédito con: timestamp, ID del documento, ID de la forma enviada, técnico ejecutor y saldo resultante | Alta |
| RF-07.12 | El sistema debe mostrar el historial de consumo de créditos filtrable por fecha, técnico y tipo de reporte | Media |
| RF-07.13 | El sistema debe ofrecer un resumen mensual de consumo de créditos y consultas por tipo de reporte para planificación del siguiente año | Baja |
| RF-07.14 | El sistema debe entregar automáticamente **4 créditos bonus** al tenant al confirmar la compra o renovación de cualquier paquete de créditos anuales, independientemente del tamaño del paquete | Alta |
| RF-07.17 | El sistema debe mantener una **tabla de costos por Perfil de Documento** configurable por el Admin CRM, con al menos: `ASISTENCIA_DIGITAL = 1.0` y `ASISTENTE_AUTOMATICO_NORMAS = 1.5`. El costo se expresa en créditos con 2 decimales (BigDecimal) | Alta |
| RF-07.18 | El saldo de créditos del tenant debe almacenarse y operar con **2 decimales de precisión** (BigDecimal) para soportar correctamente consumos fraccionados de 1.5 créditos | Alta |
| RF-07.19 | El sistema debe registrar en cada Evento de Crédito: `perfil_documento`, `costo_creditos_aplicado`, `saldo_antes`, `saldo_despues`, `tecnico_id`, `documento_id`, `timestamp`. El costo registrado es el vigente en la tabla al momento del evento | Alta |
| RF-07.15 | Los 4 créditos bonus deben activarse en el saldo del tenant en el mismo instante en que se confirma el pago del paquete anual, con la misma fecha de vencimiento que el paquete adquirido | Alta |
| RF-07.16 | El sistema debe informar al Cliente en el resumen de compra del paquete anual que recibirá 4 créditos bonus, mostrando el saldo total resultante (créditos del paquete + 4 bonus) | Media |

---

### RF-08 — Sistema de Contadores de Consultas por Tenant

> **Propósito:** Aunque las consultas no consumen crédito por sí mismas, el sistema las contabiliza para determinar si el tenant supera el volumen incluido en sus créditos. El umbral es `créditos_totales_adquiridos × 100 consultas`. El excedente se cobra retroactivamente como créditos adicionales.

#### Estructura de contadores por Tenant

```
TENANT (empresa cliente)
├── saldo_creditos_disponibles        ← créditos restantes para nuevos envíos
├── creditos_totales_adquiridos       ← base para calcular umbral de consultas
├── umbral_consultas_incluidas        ← creditos_totales_adquiridos × 100
├── contador_global_consultas         ← suma de TODAS las consultas de TODOS los reportes
│
└── contadores_por_tipo_reporte[]
    ├── tipo_reporte: "Informe Técnico"
    │   └── total_consultas: N
    ├── tipo_reporte: "Acta de Visita"
    │   └── total_consultas: N
    ├── tipo_reporte: "Reporte de Incidencia"
    │   └── total_consultas: N
    └── ... (un contador por cada tipo de reporte del CRM)
```

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-08.1 | El sistema debe mantener un **contador global de consultas** por tenant que acumule todas las consultas de todos los tipos de reporte durante el año en curso | Alta |
| RF-08.2 | El sistema debe mantener un **contador individual por tipo de reporte** por tenant, acumulando las consultas específicas de cada tipo | Alta |
| RF-08.3 | Los contadores de consultas deben incrementarse en +1 cada vez que ocurra cualquiera de los siguientes eventos sobre un documento existente: visualización, firma electrónica, descarga del PDF, reenvío, consulta de estado | Alta |
| RF-08.4 | El umbral de consultas gratuitas del tenant se calcula como: `créditos_totales_adquiridos_en_el_año × 100` | Alta |
| RF-08.5 | El sistema debe comparar el **contador global de consultas** del tenant contra su umbral; cuando el contador supere el umbral, el sistema debe cobrar **1 crédito adicional por cada 100 consultas de excedente** | Alta |
| RF-08.6 | El cobro de excedente por consultas se evalúa en tiempo real: en el momento en que la consulta número `(umbral + 1)` ocurra, se descuenta 1 crédito del saldo disponible del tenant | Alta |
| RF-08.7 | Cada bloque de 100 consultas de excedente consume exactamente 1 crédito; las consultas parciales dentro de un bloque no se cobran hasta completar las 100 | Alta |
| RF-08.8 | Los contadores de consultas se reinician a 0 al iniciar cada nuevo período anual de contrato | Alta |
| RF-08.9 | Los contadores no se reinician al comprar créditos adicionales (top-up); solo se reinician al renovar el período anual | Alta |
| RF-08.10 | El sistema debe mostrar al tenant en su panel: contador global, contador por tipo de reporte, umbral vigente, excedente acumulado y créditos consumidos por excedente en el período actual | Alta |
| RF-08.11 | El sistema debe notificar al tenant cuando el contador global de consultas alcance el 80% y el 100% del umbral incluido | Alta |
| RF-08.12 | Todos los valores de los contadores deben ser accesibles vía API para integración con herramientas de monitoreo o BI del tenant | Baja |

---

### RF-09 — Reglas de Negocio — Créditos y Contadores

| ID | Regla | Prioridad |
|----|-------|-----------|
| RN-C01 | El único evento que consume crédito del saldo es: Técnico de campo envía forma → PDF generado exitosamente. El importe descontado es el costo del Perfil de Documento activo (1.0 ó 1.5 créditos). Sin excepción | Alta |
| RN-C-PDF-01 | El Perfil **Asistencia Digital** tiene un costo fijo de **1.0 crédito** por PDF generado. Aplica al rol Técnico de campo | Alta |
| RN-C-PDF-02 | El Perfil **Asistente Automático de Normas** (antes: *Asistente de Cumplimiento de Normas*) tiene un costo fijo de **1.5 créditos** por PDF generado. Aplica al rol Técnico de campo | Alta |
| RN-C-PDF-03 | El sistema debe verificar que `saldo_disponible ≥ costo_perfil` antes de reservar el crédito. Si el saldo es insuficiente, se rechaza la operación con error `SALDO_INSUFICIENTE` e indica cuántos créditos faltan | Alta |
| RN-C-PDF-04 | La reserva y el descuento del crédito son **atómicos**: se reserva el costo completo del perfil, se intenta generar el PDF y, si falla, se reintegra el importe exacto reservado. No es posible un descuento parcial | Alta |
| RN-C02 | Firmas electrónicas, descargas de PDF, visualizaciones y cualquier acción posterior sobre el documento NO consumen crédito pero SÍ incrementan el contador de consultas | Alta |
| RN-C03 | El contador de consultas opera a nivel de tenant, no a nivel de documento individual. Un documento con 50 consultas y otro con 70 = 120 consultas del contador global del tenant | Alta |
| RN-C04 | El umbral de consultas incluidas se recalcula automáticamente si el tenant adquiere créditos adicionales (top-up): nuevo umbral = `(créditos originales + top-up) × 100` | Alta |
| RN-C05 | Si el tenant no tiene saldo de créditos cuando ocurre un cobro de excedente de consultas, el sistema debe notificar y registrar la deuda; el acceso a consultas no se bloquea por deuda de excedente | Media |
| RN-C06 | La estimación base de uso es 100 consultas/documento/año. Un tenant con 10 créditos tiene un umbral de 1.000 consultas anuales para todos sus documentos | Alta |
| RN-C07 | Los contadores de consultas son de solo lectura para el tenant; no pueden ser manipulados ni reiniciados manualmente desde el panel | Alta |
| RN-C08 | El contador por tipo de reporte no afecta el cobro de excedente; solo el contador global del tenant determina si hay excedente | Alta |
| RN-C09 | Un Cliente puede tener simultáneamente una Licencia por Módulo (suscripción) y Licencia por Créditos; son complementarias | Alta |
| RN-C10 | No se realizan reembolsos monetarios por créditos no consumidos al vencimiento del período anual | Media |
| RN-C11 | Los 4 créditos bonus se entregan en toda compra o renovación de paquete anual de créditos, sin excepción de tamaño ni modalidad del paquete | Alta |
| RN-C12 | Los 4 créditos bonus no generan un paquete independiente; se fusionan con el saldo general del tenant y su vencimiento es idéntico al del paquete que los origina | Alta |
| RN-C13 | La tarifa de renovación anual de 99 € aplica a todos los contratos anuales del sistema (módulos y paquetes de créditos); es un cargo fijo que no varía con el tamaño del paquete ni el número de módulos contratados | Alta |
| RN-C14 | Si el Cliente tiene múltiples contratos anuales que vencen en la misma fecha, se genera un cargo de 99 € por cada contrato renovado, no un único cargo consolidado | Alta |

---

### RF-09 — Apertura de Contrato

> **Descripción del modelo:** Todo Cliente nuevo debe realizar un pago único de Apertura de Contrato antes de acceder al CRM. La apertura determina la modalidad de reporte que tendrá disponible y otorga créditos de bienvenida incluidos en el precio.

#### Tabla comparativa de modalidades

| Concepto | Apertura Estándar | Apertura Personalizada |
|----------|------------------|----------------------|
| **Precio** | 400 € (único) | 1.400 € (único) |
| **Créditos incluidos** | 2 créditos | 10 créditos |
| **Tipo de reporte** | Reporte estándar del sistema | Reporte con marca del Cliente |
| **Logotipo en reporte** | No | Sí |
| **Cabecera personalizada** | No | Sí |
| **Pie de página personalizado** | No | Sí |
| **Secciones a medida** | No | Sí (configurables) |
| **Tiempo de setup** | Inmediato (automático) | Hasta 5 días hábiles |

---

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-09.1 | El sistema debe requerir el pago de Apertura de Contrato como paso obligatorio previo al acceso al CRM para todo Cliente nuevo | Alta |
| RF-09.2 | El sistema debe presentar al Cliente las dos modalidades de apertura (Estándar y Personalizada) con una comparativa clara de diferencias, precio y créditos incluidos | Alta |
| RF-09.3 | El pago de apertura es un cargo único no recurrente; no forma parte del ciclo de suscripción mensual/anual | Alta |
| RF-09.4 | Los créditos de bienvenida incluidos en la apertura deben activarse automáticamente al confirmar el pago | Alta |
| RF-09.5 | Los créditos de bienvenida no caducan antes de los primeros 12 meses de contrato activo | Alta |
| RF-09.6 | La Apertura Estándar debe dejar al Cliente operativo en menos de 60 minutos de forma completamente automatizada | Alta |
| RF-09.7 | La Apertura Personalizada debe iniciar un flujo de onboarding asistido donde el Cliente puede subir su logotipo, definir cabecera, pie de página y las secciones que desea incluir en su reporte | Alta |
| RF-09.8 | El sistema debe permitir al Cliente subir su logotipo en formatos PNG, SVG o JPG (mínimo 300px de ancho, fondo transparente recomendado) | Alta |
| RF-09.9 | El sistema debe permitir al Cliente definir el texto de cabecera y pie de página del reporte (máximo 200 caracteres cada uno) | Alta |
| RF-09.10 | El sistema debe permitir al Cliente crear secciones personalizadas en el reporte, indicando nombre y tipo de contenido (tabla, gráfico, texto libre, KPI) | Alta |
| RF-09.11 | El equipo interno de Mikel debe revisar y validar la configuración personalizada antes de activarla; plazo máximo de 5 días hábiles | Alta |
| RF-09.12 | El Cliente debe recibir una vista previa del reporte personalizado para su aprobación antes de la activación definitiva | Media |
| RF-09.13 | El Cliente puede solicitar hasta 2 rondas de ajustes sobre el reporte personalizado sin costo adicional dentro de los primeros 30 días | Media |
| RF-09.14 | El sistema debe emitir una factura del pago de apertura separada de las facturas de suscripción periódica | Alta |
| RF-09.15 | Una vez activada la modalidad de apertura, el Cliente puede hacer upgrade de Estándar a Personalizada pagando la diferencia (1.000 €) más los 8 créditos adicionales | Media |

---

### RF-10 — Reglas de Negocio — Apertura de Contrato

| ID | Regla | Prioridad |
|----|-------|-----------|
| RF-10.1 | No existe acceso al CRM sin Apertura de Contrato pagada; el trial (US-03) no está disponible para módulos hasta completar la apertura | Alta |
| RF-10.2 | La Apertura de Contrato es un pago único por empresa; no se repite aunque el Cliente añada usuarios o módulos | Alta |
| RF-10.3 | El pago de apertura no es reembolsable una vez iniciado el proceso de configuración (Estándar: inmediato; Personalizada: al subir el primer activo) | Alta |
| RF-10.4 | Los créditos de bienvenida se suman al saldo general de créditos; su consumo sigue las mismas reglas que los créditos regulares | Alta |
| RF-10.5 | El upgrade de Estándar a Personalizada requiere pago de diferencia (1.000 €) y 8 créditos adicionales; no obliga a reiniciar la suscripción activa | Media |
| RF-10.6 | La modalidad de apertura elegida (Estándar o Personalizada) determina el tipo de reporte disponible para todos los módulos del Cliente | Alta |

---

### RF-11 — Catálogo de Módulos del Mikel CRM

> Esta sección define los módulos disponibles, su descripción funcional y su modalidad de licencia. Cada módulo se activa de forma independiente.

#### Módulo: Administrador (Supervisor / Coordinador)

| Atributo | Valor |
|----------|-------|
| **Nombre comercial** | Administrador |
| **Perfil objetivo** | Supervisor / Coordinador |
| **Modalidad de licencia** | Contrato Anual con Cuotas Mensuales (CACM) |
| **Precio** | 200 € / mes |
| **Compromiso** | 12 meses (permanencia mínima) |
| **Coste total anual** | 2.400 € |
| **Créditos incluidos** | ⚠️ Pendiente de definición comercial |
| **Penalización por baja anticipada** | ⚠️ Pendiente de definición comercial |

**Funcionalidades incluidas:**

| # | Funcionalidad | Descripción |
|---|--------------|-------------|
| F-ADM-01 | Tablero en tiempo real | Vista centralizada con el estado de todo el equipo: técnicos activos, tickets en curso, documentos pendientes de aprobación |
| F-ADM-02 | Gestión de tickets solicitud→entrega | Ciclo de vida completo del ticket: creación de solicitud, asignación a técnico, seguimiento de estados, cierre y entrega |
| F-ADM-03 | Aprobación de documentos | El Supervisor revisa los documentos generados por el equipo técnico antes de que sean enviados al cliente final |
| F-ADM-04 | eFirma de Supervisor | Firma electrónica del Supervisor integrada en el flujo de aprobación; genera un registro inmutable de la validación |
| F-ADM-05 | Historial completo por cliente | Registro auditable de todos los tickets, documentos, firmas y estados asociados a cada cliente, accesible para auditorías |

---

### RF-12 — Licencia Módulo Administrador (Contrato Anual)

> El Módulo Administrador sigue el **modelo universal de Contrato Anual con Cuotas Mensuales** definido en RF-02. Los requerimientos específicos de este módulo son los funcionales (tablero, tickets, aprobación); las reglas de pago, renovación, suspensión y almacenamiento son las mismas que aplican a todos los contratos del sistema.

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-12.1 | El Módulo Administrador se contrata bajo el modelo universal: Contrato Anual con cuota mensual de **200 €** (coste total anual: 2.400 €) | Alta |
| RF-12.2 | Al contratar, el sistema debe mostrar: cuota mensual (200 €), duración (12 meses), coste total (2.400 €), fecha de inicio, fecha de vencimiento y tarifa de renovación anual (99 €) | Alta |
| RF-12.3 | El acceso al Módulo Administrador se activa inmediatamente tras el pago de la primera cuota mensual | Alta |
| RF-12.4 | El panel de licencias muestra el estado del contrato del Módulo Administrador: cuotas pagadas (N/12), próxima fecha de cobro, fecha de vencimiento y estado de renovación automática | Alta |
| RF-12.5 | La Ventana de Renovación (días -30, -7 y -1) y el proceso de renovación automática con tarifa de 99 € siguen exactamente las reglas de RF-02.4 a RF-02.8 | Alta |
| RF-12.6 | La gestión de cuotas mensuales impagadas, suspensión y Cuota de Almacenamiento siguen exactamente las reglas de RF-04.1 a RF-04.7 y RF-14 | Alta |
| RF-12.7 | La baja anticipada del módulo antes de completar los 12 meses está sujeta a las condiciones de RF-06.8 ⚠️ *(penalización por baja anticipada pendiente de definición comercial — RN-ADM-05)* | Alta |

---

### RF-13 — Reglas de Negocio — Módulo Administrador (CACM)

| ID | Regla | Prioridad |
|----|-------|-----------|
| RN-ADM-01 | El CACM es un contrato de 12 meses; el cliente no puede reducir la duración una vez firmado | Alta |
| RN-ADM-02 | La cuota mensual de 200 € es fija durante los 12 meses; no está sujeta a variación de precio dentro del período contratado | Alta |
| RN-ADM-03 | El inicio del contrato es la fecha del primer pago exitoso; el vencimiento es exactamente 12 meses después | Alta |
| RN-ADM-04 | La suspensión por fallo de pago en una cuota mensual no cancela el contrato anual; el cliente sigue obligado a las cuotas pendientes | Alta |
| RN-ADM-05 | ⚠️ **PENDIENTE DEFINICIÓN COMERCIAL**: política de penalización por baja anticipada antes de completar los 12 meses | Alta |
| RN-ADM-06 | ⚠️ **PENDIENTE DEFINICIÓN COMERCIAL**: si el módulo incluye créditos de uso o solo acceso funcional | Media |
| RN-ADM-07 | ⚠️ **PENDIENTE DEFINICIÓN COMERCIAL**: precio y modalidad disponibles para renovación tras completar los 12 meses (¿continúa en CACM, pasa a mensual renovable, o precio diferente?) | Media |
| RN-ADM-08 | La eFirma de Supervisor emitida dentro del módulo se rige por las mismas reglas de contabilización de consultas que el resto de documentos del tenant (RF-08) | Alta |

---

### RF-14 — Política de Descuentos por Puntualidad de Pago

> Esta política aplica a todas las cuotas y facturas del sistema: suscripciones mensuales, cuotas CACM, renovaciones anuales y paquetes de créditos.

#### Ventana de pago y descuentos

| Momento del pago | Día relativo a Fecha Límite | Descuento aplicado | Estado del servicio |
|------------------|----------------------------|--------------------|---------------------|
| Pago anticipado | Cualquier día antes del día 0 | **10 %** sobre el monto | Activo |
| Pago en fecha límite | Día 0 | **3 %** sobre el monto | Activo |
| Pago en gracia | Día +1 | Sin descuento (precio normal) | Activo |
| Pago vencido | Día +2 en adelante | Sin descuento + **Cuota de Almacenamiento** | **Suspendido** |

#### Fórmula de la Cuota de Almacenamiento (suspensión por mora)

```
Cuota_Almacenamiento (€/mes) = 50 + CEIL(MAX(reportes_almacenados - 100, 0) / 100) × 10

Ejemplos:
  reportes =   0 – 100  →  50 €/mes
  reportes = 101 – 200  →  60 €/mes
  reportes = 201 – 300  →  70 €/mes
  reportes = 301 – 400  →  80 €/mes
  (y así sucesivamente, +10 € por cada 100 reportes o fracción adicional)
```

| ID | Requerimiento | Prioridad |
|----|--------------|-----------|
| RF-14.1 | El sistema debe aplicar automáticamente un descuento del **10 %** sobre el monto de cualquier factura cuyo pago se registre antes de la Fecha Límite | Alta |
| RF-14.2 | El sistema debe aplicar automáticamente un descuento del **3 %** sobre el monto de cualquier factura cuyo pago se registre el mismo día de la Fecha Límite | Alta |
| RF-14.3 | El sistema debe cobrar el monto íntegro sin descuento cuando el pago se registre exactamente 1 día después de la Fecha Límite (día de gracia) | Alta |
| RF-14.4 | A partir del día +2 posterior a la Fecha Límite sin pago registrado, el sistema debe suspender el servicio del tenant y generar automáticamente una **Cuota de Almacenamiento mensual** | Alta |
| RF-14.5 | La Cuota de Almacenamiento base es de **50 € por mes** para tenants con hasta 100 reportes almacenados; se incrementa en **10 € por cada 100 reportes adicionales** (o fracción) por encima de los primeros 100 | Alta |
| RF-14.6 | El sistema debe calcular el número de reportes almacenados del tenant en el momento de generar cada Cuota de Almacenamiento; el monto puede variar mes a mes en función del volumen de reportes | Alta |
| RF-14.7 | El sistema debe generar y emitir la Cuota de Almacenamiento como factura independiente el primer día de cada mes de suspensión activa | Alta |
| RF-14.8 | El sistema debe mostrar en el panel del tenant: días de atraso, cuotas vencidas pendientes, Cuotas de Almacenamiento acumuladas y monto total requerido para reactivar el servicio | Alta |
| RF-14.9 | Para reactivar el servicio tras una suspensión por mora, el tenant debe realizar un único pago que cubra: la suma de todas las cuotas vencidas pendientes + la suma de todas las Cuotas de Almacenamiento acumuladas durante el período de suspensión | Alta |
| RF-14.10 | El servicio debe restaurarse en menos de 60 segundos tras confirmar el pago de reactivación completo | Alta |
| RF-14.11 | El sistema debe notificar al tenant por correo en el día +2 (inicio de suspensión) indicando: monto de cuotas vencidas, Cuota de Almacenamiento aplicable (con desglose por reportes), monto total para reactivar y enlace de pago directo | Alta |
| RF-14.12 | El sistema debe notificar al tenant por correo el primer día de cada mes adicional de suspensión con la Cuota de Almacenamiento acumulada actualizada | Alta |

---

### RF-15 — Reglas de Negocio — Política de Pagos y Almacenamiento

| ID | Regla | Prioridad |
|----|-------|-----------|
| RN-PAG-01 | El descuento por puntualidad se aplica en el momento del cobro; no es retroactivo ni acumulable con otros descuentos | Alta |
| RN-PAG-02 | El día de gracia (día +1) no genera descuento ni penalización; es simplemente el último día para pagar al precio normal antes de la suspensión | Alta |
| RN-PAG-03 | La Cuota de Almacenamiento se calcula sobre el total de reportes almacenados del tenant, no solo los generados durante el período de mora | Alta |
| RN-PAG-04 | Un tenant suspendido puede seguir consultando (solo lectura) sus reportes existentes para auditoría, pero no puede generar nuevos reportes ni ejecutar utilizaciones | Media |
| RN-PAG-05 | Las Cuotas de Almacenamiento acumuladas no caducan; permanecen como deuda hasta que el tenant pague la reactivación o cancele definitivamente la cuenta | Alta |
| RN-PAG-06 | Si el tenant cancela la cuenta durante la suspensión, la deuda de Cuotas de Almacenamiento sigue vigente durante los 90 días de retención de datos antes del purge | Media |
| RN-PAG-07 | El descuento del 10 % por pago anticipado aplica independientemente de cuántos días antes se pague; el único requisito es que sea antes de la Fecha Límite | Alta |
| RN-PAG-08 | Los descuentos por puntualidad (10 % y 3 %) se reflejan en la factura emitida; el monto facturado es el monto descontado, no el monto original | Alta |

---

## Requerimientos No Funcionales

| ID | Categoría | Requerimiento | Métrica |
|----|-----------|---------------|---------|
| RNF-01 | Rendimiento | La validación de acceso a un módulo debe resolverse en menos de 100 ms | p99 < 100 ms |
| RNF-02 | Disponibilidad | El servicio de licencias debe estar disponible 99.9% del tiempo mensual | < 44 min downtime/mes |
| RNF-03 | Seguridad | Todos los datos de pago deben ser manejados por el proveedor de pagos externo (PCI-DSS); el sistema solo almacena tokens | Auditoría anual |
| RNF-04 | Auditoría | Toda transición de estado de licencia debe quedar registrada en un log inmutable con timestamp y actor | 100% de eventos auditados |
| RNF-05 | Usabilidad | El flujo de activación de un módulo no debe requerir más de 3 pasos desde el panel del CRM | Test con usuario ≤ 3 clicks |
| RNF-06 | Escalabilidad | El sistema debe soportar hasta 5,000 suscripciones activas simultáneas sin degradación | Load test |
| RNF-07 | Privacidad | El sistema debe cumplir con la política de retención de datos (90 días post-cancelación) y notificar al usuario antes del purge | GDPR / Ley local |
| RNF-08 | Notificaciones | Los correos de alerta deben enviarse dentro de los 5 minutos de ocurrido el evento | Monitoreo de cola |

---

## Épicas e Historias de Usuario

---

### ÉPICA 1 — Activación de Módulos

> **Como** Cliente, **quiero** poder activar módulos del CRM de forma independiente **para** adaptar la herramienta exactamente a lo que mi empresa necesita, sin pagar por funcionalidades que no uso.

---

**US-01 — Ver catálogo de módulos disponibles**

> **Como** Cliente, **quiero** ver todos los módulos disponibles del CRM con su descripción, precio mensual y precio anual, **para** poder comparar opciones y decidir qué activar.

**Criterios de Aceptación:**
- [ ] El panel de licencias muestra todos los módulos disponibles, incluyendo los ya activos y los no contratados.
- [ ] Cada módulo muestra: nombre, descripción corta, precio mensual y precio anual (con % de ahorro respecto al mensual).
- [ ] Los módulos ya activos están visualmente diferenciados (estado: "Activo", "Trial", "Suspendido").
- [ ] El catálogo está ordenado por categoría o relevancia.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-02 — Activar un módulo nuevo**

> **Como** Cliente, **quiero** activar un módulo desde el panel en máximo 3 pasos, **para** comenzar a usarlo sin fricción.

**Criterios de Aceptación:**
- [ ] El flujo de activación tiene máximo 3 pasos: (1) Seleccionar módulo, (2) Elegir ciclo y confirmar precio, (3) Confirmar pago.
- [ ] Si el Cliente ya tiene un método de pago guardado, no se le solicita ingresarlo de nuevo.
- [ ] El módulo queda activo y accesible en menos de 60 segundos tras el pago confirmado.
- [ ] El Cliente recibe un correo de confirmación con el detalle del módulo activado y el próximo cargo.
- [ ] Si el pago falla, se muestra un mensaje de error específico y se ofrece actualizar el método de pago.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-03 — Iniciar prueba gratuita de un módulo (Trial)**

> **Como** Cliente que nunca ha usado el módulo de Reportes, **quiero** iniciar un trial de 14 días sin ingresar datos de pago, **para** evaluar si vale la pena contratarlo.

**Criterios de Aceptación:**
- [ ] El botón "Probar gratis 14 días" aparece solo en módulos que la empresa nunca ha contratado.
- [ ] No se solicita método de pago para iniciar el trial.
- [ ] Al iniciar el trial, el módulo queda activo con una etiqueta "Trial — X días restantes" visible en el panel.
- [ ] El sistema notifica al Cliente en el día 10 del trial que quedan 4 días y ofrece contratar.
- [ ] Al vencer el trial sin contratar, el acceso al módulo se bloquea automáticamente.
- [ ] No es posible iniciar un segundo trial del mismo módulo en la misma empresa.

**Prioridad:** Media | **Estimación:** 5 pts

---

**US-04 — Ver aviso al intentar acceder a módulo no licenciado**

> **Como** usuario del CRM que intenta abrir el módulo de Soporte sin licencia activa, **quiero** ver un aviso claro con opción de activarlo, **para** no perderme en la aplicación.

**Criterios de Aceptación:**
- [ ] Al acceder a un módulo sin licencia, el sistema muestra una pantalla de bloqueo (no un error genérico).
- [ ] La pantalla de bloqueo incluye: nombre del módulo, beneficios clave y botón "Activar este módulo".
- [ ] Si el usuario tiene rol de administrador de la cuenta, el botón inicia el flujo de activación (US-02).
- [ ] Si el usuario no tiene permiso para contratar, el mensaje indica que debe contactar al administrador de la cuenta.

**Prioridad:** Media | **Estimación:** 3 pts

---

### ÉPICA 2 — Gestión de Suscripción

> **Como** Cliente, **quiero** gestionar mis módulos activos y mi ciclo de facturación **para** tener control total sobre qué pago y cuándo.

---

**US-05 — Ver panel de licencias activas**

> **Como** Cliente, **quiero** ver de un vistazo todos mis módulos activos, sus fechas de vencimiento y el próximo monto a cobrar, **para** tener control claro de mi suscripción.

**Criterios de Aceptación:**
- [ ] El panel muestra cada módulo activo con: nombre, estado, ciclo (mensual/anual), fecha de próximo cobro y monto.
- [ ] El panel muestra el total consolidado a cobrar en el próximo ciclo.
- [ ] Los módulos próximos a vencer (≤ 7 días) se resaltan visualmente con alerta.
- [ ] Existe un acceso rápido a "Agregar módulo" y a "Ver historial de pagos".

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-06 — Agregar módulo a suscripción existente (Upgrade)**

> **Como** Cliente con suscripción activa, **quiero** agregar el módulo de Ventas a mi plan sin cancelar los módulos actuales, **para** ampliar las capacidades del CRM según crezca mi empresa.

**Criterios de Aceptación:**
- [ ] El sistema calcula y muestra el cargo prorrateado del nuevo módulo por los días restantes del ciclo actual antes de confirmar.
- [ ] El módulo nuevo queda activo inmediatamente tras confirmar el pago.
- [ ] El próximo ciclo de facturación incluye el módulo nuevo al precio completo.
- [ ] Si el módulo seleccionado ya está activo, no se permite duplicar y se muestra un mensaje informativo.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-07 — Desactivar un módulo (Downgrade)**

> **Como** Cliente, **quiero** desactivar el módulo de Reportes que ya no uso, **para** reducir el costo de mi suscripción al renovar el plan anual.

**Criterios de Aceptación:**
- [ ] La opción de downgrade solo está disponible cuando el contrato del módulo está en su **año de vigencia activo**; no se puede solicitar en ningún momento intermedio del año.
- [ ] El sistema bloquea la solicitud de downgrade si el contrato no es de modalidad anual o si quedan menos de 24 horas para el vencimiento (ya cubierto por el flujo de renovación).
- [ ] Al solicitar el downgrade, el sistema informa al Cliente: "El módulo seguirá activo hasta el [fecha de vencimiento anual]. No se realizará la renovación automática de este módulo."
- [ ] El sistema informa el monto que dejará de cobrarse en la próxima renovación anual.
- [ ] No hay reembolso por el período anual ya pagado.
- [ ] El Cliente puede cancelar la solicitud de downgrade hasta 24 horas antes del vencimiento anual.
- [ ] Al vencer el año, el módulo se desactiva automáticamente y no se incluye en la siguiente renovación.

**Prioridad:** Media | **Estimación:** 5 pts

---

**~~US-08 — Cambiar de ciclo mensual a anual~~** ⛔ ELIMINADA

> **Motivo:** Todos los contratos son anuales desde el momento de la contratación. No existe ciclo mensual renovable independiente. Esta historia queda obsoleta con el modelo universal definido en RF-02. Los puntos de estimación (5 pts) se restan del total del backlog.

---

### ÉPICA 3 — Renovación y Pagos

> **Como** Cliente, **quiero** que mis suscripciones se gestionen de forma transparente y recibir avisos oportunos, **para** nunca perder el acceso por descuido y tener visibilidad de lo que pago.

---

**US-09 — Recibir alertas de vencimiento próximo**

> **Como** Cliente con suscripción mensual, **quiero** recibir avisos por correo cuando mi licencia esté por vencer, **para** renovarla a tiempo o tomar una decisión de cancelar conscientemente.

**Criterios de Aceptación:**
- [ ] El sistema envía un correo al contacto principal 30 días antes del vencimiento.
- [ ] El sistema envía un segundo correo 7 días antes del vencimiento.
- [ ] El sistema envía un tercer correo 1 día antes del vencimiento.
- [ ] Cada correo incluye: módulos activos, fecha de vencimiento, monto a cobrar, **tarifa de renovación anual aplicable (99 €)** y enlace directo al panel.
- [ ] Los correos se envían en el idioma configurado en la cuenta del Cliente.
- [ ] Si la renovación automática está activa, el correo lo indica claramente ("Tu suscripción se renovará automáticamente el [fecha]. Se cobrarán 99 € de tarifa de renovación más el coste del contrato correspondiente").

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-10 — Actualizar método de pago**

> **Como** Cliente cuya tarjeta está por vencer, **quiero** actualizar mis datos de pago en el panel, **para** asegurar que la renovación automática no falle.

**Criterios de Aceptación:**
- [ ] El Cliente puede acceder a la sección de pago desde el panel de licencias.
- [ ] El sistema muestra el método de pago actual de forma enmascarada (últimos 4 dígitos).
- [ ] El Cliente puede agregar un nuevo método de pago y establecerlo como predeterminado.
- [ ] El sistema valida el nuevo método de pago con una transacción de $0 o micro-cargo reembolsable.
- [ ] Los datos de la tarjeta son procesados directamente por el proveedor de pagos; el sistema no almacena el número completo.
- [ ] El cliente recibe confirmación por correo al actualizar el método de pago exitosamente.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-11 — Gestionar falla de pago y ventana de descuentos**

> **Como** Cliente, **quiero** que el sistema aplique automáticamente el descuento correcto según cuándo pague y me notifique claramente las consecuencias de no pagar a tiempo, **para** aprovechar los beneficios de puntualidad y evitar la suspensión del servicio.

**Criterios de Aceptación:**

*Descuentos por puntualidad:*
- [ ] Si el pago se registra **antes** de la Fecha Límite, el sistema aplica automáticamente un **10 % de descuento** y emite la factura por el monto descontado.
- [ ] Si el pago se registra **el mismo día** de la Fecha Límite, el sistema aplica un **3 % de descuento** y emite la factura por el monto descontado.
- [ ] Si el pago se registra el **día +1** (día de gracia), el sistema cobra el monto íntegro sin descuento ni penalización.
- [ ] Cada correo de alerta de vencimiento (30, 7 y 1 día antes) indica el descuento disponible si se paga antes de la fecha límite.

*Suspensión por mora (día +2 en adelante):*
- [ ] En el día +2 sin pago, el sistema suspende automáticamente el servicio y bloquea el acceso operativo (generación de reportes, utilizaciones); la consulta de reportes existentes en modo solo lectura permanece disponible.
- [ ] El sistema genera inmediatamente una **Cuota de Almacenamiento** calculada sobre el número de reportes almacenados del tenant: 50 € base para ≤ 100 reportes, +10 € por cada 100 reportes adicionales (o fracción).
- [ ] El sistema notifica al Cliente en el día +2 con: monto de cuotas vencidas, Cuota de Almacenamiento detallada (desglose por reportes), monto total para reactivar y enlace de pago directo.
- [ ] El sistema genera una nueva Cuota de Almacenamiento el primer día de cada mes adicional de suspensión, actualizada según el volumen de reportes vigente.
- [ ] El panel muestra en todo momento: días de atraso, cuotas vencidas, Cuotas de Almacenamiento acumuladas y el **total a pagar para reactivar**.

*Reactivación:*
- [ ] El sistema permite la reactivación únicamente tras el pago completo de: todas las cuotas vencidas pendientes + todas las Cuotas de Almacenamiento acumuladas.
- [ ] El servicio se restaura en menos de 60 segundos tras confirmar el pago total de reactivación.
- [ ] El Cliente recibe correo de confirmación de reactivación con el resumen de lo pagado y la nueva fecha de vencimiento.

**Prioridad:** Alta | **Estimación:** 13 pts

---

**US-12 — Desactivar renovación automática**

> **Como** Cliente, **quiero** poder desactivar la renovación automática de mi suscripción, **para** tener control sobre cuándo y si renuevo.

**Criterios de Aceptación:**
- [ ] El Cliente puede desactivar la renovación automática desde el panel de licencias.
- [ ] La opción solo está disponible con al menos 24 horas de anticipación al próximo cobro.
- [ ] Al desactivarla, el sistema muestra un aviso claro: "Tu acceso terminará el [fecha]. No se realizará ningún cargo automático."
- [ ] El acceso continúa hasta la fecha de vencimiento pagada.
- [ ] El Cliente puede volver a activar la renovación automática en cualquier momento antes del vencimiento.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-13 — Descargar historial de pagos y facturas**

> **Como** Cliente, **quiero** ver y descargar el historial de todos mis pagos en formato PDF, **para** tener mis comprobantes fiscales en orden.

**Criterios de Aceptación:**
- [ ] El historial muestra todos los cargos exitosos con: fecha, módulos incluidos, monto, ciclo y estado (pagado/fallido).
- [ ] Cada pago exitoso tiene disponible su factura descargable en PDF.
- [ ] El historial es filtrable por rango de fechas.
- [ ] Las facturas incluyen: datos fiscales del Cliente, detalle de módulos, período facturado y número de factura único.

**Prioridad:** Media | **Estimación:** 5 pts

---

### ÉPICA 6 — Apertura de Contrato

> **Como** Cliente nuevo, **quiero** formalizar mi acceso al Mikel CRM eligiendo la modalidad de apertura que mejor se adapta a mi empresa, **para** comenzar a operar con el reporte adecuado a mis necesidades desde el primer día.

---

**US-24 — Elegir modalidad de apertura de contrato**

> **Como** Cliente nuevo, **quiero** ver una comparativa clara entre la Apertura Estándar (400 €) y la Personalizada (1.400 €) con sus diferencias y créditos incluidos, **para** tomar una decisión informada antes de pagar.

**Criterios de Aceptación:**
- [ ] Al registrarse, el Cliente es dirigido al paso de Apertura de Contrato antes de acceder a cualquier funcionalidad del CRM.
- [ ] Se muestra una tabla comparativa con: precio, créditos incluidos, tipo de reporte, características de personalización y tiempo de activación de cada modalidad.
- [ ] Se destacan visualmente los beneficios diferenciadores de la modalidad Personalizada (logotipo, cabecera, pie de página, secciones a medida).
- [ ] Ambas opciones muestran el monto exacto en euros y el desglose de créditos de bienvenida.
- [ ] El Cliente puede acceder a una vista previa de cómo luce cada tipo de reporte (ejemplo con datos ficticios) antes de decidir.
- [ ] El sistema no permite avanzar al CRM sin completar la selección y el pago.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-25 — Contratar Apertura Estándar (400 €)**

> **Como** Cliente nuevo que no necesita personalización de marca en los reportes, **quiero** activar la Apertura Estándar y comenzar a usar el CRM de forma inmediata, **para** no esperar ni pagar más de lo necesario.

**Criterios de Aceptación:**
- [ ] El flujo de pago de Apertura Estándar tiene máximo 2 pasos: (1) Confirmar selección y precio, (2) Pagar.
- [ ] Tras el pago confirmado, el CRM queda operativo en menos de 60 minutos de forma automatizada.
- [ ] Los 2 créditos de bienvenida quedan disponibles en el saldo del Cliente al activarse el acceso.
- [ ] El Cliente recibe correo de bienvenida con: resumen del plan contratado, créditos disponibles, fecha de activación y enlace para ingresar al CRM.
- [ ] Se emite factura separada por el pago de apertura (no incluida en facturas de suscripción).
- [ ] El reporte generado por el sistema usa la plantilla estándar de Mikel CRM (sin logo, cabecera ni pie de página del Cliente).

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-26 — Contratar Apertura Personalizada (1.400 €)**

> **Como** Cliente nuevo que quiere que el CRM refleje la identidad visual de mi empresa, **quiero** pagar la Apertura Personalizada y configurar el reporte con mi logotipo, cabecera, pie de página y secciones propias, **para** entregar reportes de calidad profesional a mis stakeholders.

**Criterios de Aceptación:**
- [ ] El flujo de pago de Apertura Personalizada tiene 3 pasos: (1) Confirmar selección y precio, (2) Pagar, (3) Iniciar onboarding de personalización.
- [ ] Tras el pago, se activan los 10 créditos de bienvenida inmediatamente, incluso antes de completar la personalización.
- [ ] El onboarding de personalización permite al Cliente:
  - Subir logotipo (PNG, SVG o JPG; mínimo 300px de ancho).
  - Definir texto de cabecera (máx. 200 caracteres).
  - Definir texto de pie de página (máx. 200 caracteres).
  - Crear hasta 10 secciones personalizadas indicando nombre y tipo (tabla, gráfico, texto libre o KPI).
- [ ] El sistema valida el formato del logotipo al momento de la carga y muestra error descriptivo si no cumple los requisitos.
- [ ] El Cliente accede al CRM en modo limitado (sin reporte personalizado activo) mientras el equipo de Mikel valida la configuración.
- [ ] El equipo interno de Mikel activa el reporte personalizado en un plazo máximo de 5 días hábiles.
- [ ] El Cliente recibe notificación cuando el reporte personalizado queda listo para revisión.

**Prioridad:** Alta | **Estimación:** 8 pts

---

**US-27 — Aprobar vista previa del reporte personalizado**

> **Como** Cliente con Apertura Personalizada en proceso, **quiero** revisar una vista previa de mi reporte antes de que quede activo en el CRM, **para** confirmar que la personalización cumple mis expectativas o solicitar ajustes.

**Criterios de Aceptación:**
- [ ] El sistema notifica al Cliente (in-app + correo) cuando la vista previa del reporte personalizado está disponible para revisión.
- [ ] El Cliente puede visualizar el reporte con sus datos reales (o datos de muestra si aún no hay datos cargados).
- [ ] El Cliente tiene dos opciones: "Aprobar y activar" o "Solicitar ajustes".
- [ ] Si solicita ajustes, puede detallar los cambios en un campo de texto; el sistema notifica al equipo de Mikel.
- [ ] El Cliente puede solicitar hasta 2 rondas de ajustes sin costo adicional dentro de los primeros 30 días post-pago.
- [ ] Una tercera ronda de ajustes o ajustes fuera de los 30 días se tratan como un nuevo servicio con cargo adicional.
- [ ] Al aprobar, el reporte personalizado queda activo en menos de 24 horas.

**Prioridad:** Media | **Estimación:** 5 pts

---

**US-28 — Upgrade de Apertura Estándar a Personalizada**

> **Como** Cliente con Apertura Estándar que ha decidido incorporar la identidad visual de su empresa al CRM, **quiero** hacer upgrade a la modalidad Personalizada pagando solo la diferencia, **para** no perder lo ya invertido.

**Criterios de Aceptación:**
- [ ] La opción de upgrade está disponible desde el panel de licencias bajo "Mi Plan".
- [ ] El sistema muestra el costo del upgrade: 1.000 € (diferencia) + 8 créditos adicionales incluidos.
- [ ] La suscripción y módulos activos no se interrumpen durante el proceso de upgrade.
- [ ] Tras el pago del upgrade, se inicia el flujo de onboarding de personalización (igual que US-26, paso 3).
- [ ] Se emite factura separada por el monto del upgrade.
- [ ] Los 8 créditos adicionales quedan disponibles inmediatamente tras el pago del upgrade.

**Prioridad:** Media | **Estimación:** 5 pts

---

### ÉPICA 5 — Licencia por Créditos Anuales

> **Como** Cliente, **quiero** adquirir créditos anuales y consumirlos según lo que use, **para** pagar únicamente por lo que ejecuto en el CRM, con visibilidad total de mi saldo en todo momento.

---

**US-16 — Adquirir paquete de créditos anuales**

> **Como** Cliente, **quiero** comprar un paquete de créditos anuales eligiendo el tamaño que mejor se adapta a mi volumen de uso, **para** activar acciones específicas del CRM durante los próximos 12 meses.

**Criterios de Aceptación:**
- [ ] El sistema muestra los paquetes disponibles con: cantidad de créditos, precio total, precio por crédito y fecha de vencimiento (hoy + 12 meses).
- [ ] El sistema muestra el ahorro por crédito en paquetes más grandes respecto al más pequeño.
- [ ] La pantalla de confirmación de compra muestra claramente: créditos del paquete + **4 créditos bonus** = saldo total que recibirá el Cliente.
- [ ] Tras el pago confirmado, los créditos del paquete **y los 4 créditos bonus** quedan disponibles en el saldo del tenant en menos de 60 segundos.
- [ ] El Cliente recibe correo con: créditos adquiridos, créditos bonus incluidos (4), saldo total resultante, fecha de vencimiento y enlace al panel.
- [ ] El panel muestra inmediatamente el saldo actualizado con indicador visual (ej: barra de progreso).
- [ ] La renovación automática del paquete al año siguiente cobrará 99 € (tarifa de renovación); el Cliente puede desactivarla con al menos 24 horas de anticipación.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-17 — Ver saldo de créditos en tiempo real**

> **Como** Cliente usando el CRM, **quiero** ver en todo momento cuántos créditos me quedan y cuándo vencen, **para** planificar mi uso y evitar sorpresas.

**Criterios de Aceptación:**
- [ ] El saldo de créditos disponibles es visible en el panel principal del CRM de forma permanente.
- [ ] El saldo muestra: créditos disponibles, créditos consumidos (del total), fecha de vencimiento del paquete y días restantes.
- [ ] El saldo se actualiza en tiempo real (o con un máximo de 5 segundos de delay) tras cada utilización.
- [ ] Cuando el saldo es ≤ 20% del total adquirido, el indicador cambia a color de advertencia (amarillo).
- [ ] Cuando el saldo es ≤ 10% del total adquirido, el indicador cambia a crítico (rojo) y aparece un banner de aviso.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-18 — Consumir un crédito al enviar una forma y generar el PDF**

> **Como** técnico del CRM, **quiero** enviar una forma y que el sistema genere el PDF descontando 1 crédito de forma transparente, **para** tener claro qué acción cuesta crédito y que el resto del flujo del documento no me genere cargos adicionales.

**Criterios de Aceptación:**
- [ ] El sistema reserva 1 crédito del saldo del tenant antes de iniciar la generación del PDF (previene doble consumo).
- [ ] Si la generación del PDF falla por cualquier causa técnica, el crédito reservado se reintegra automáticamente en ≤ 30 segundos y se notifica al técnico.
- [ ] Tras la generación exitosa del PDF, el sistema muestra: "Documento generado — 1 crédito consumido — Saldo restante: X créditos".
- [ ] El registro del evento incluye: timestamp, ID del documento, nombre de la forma enviada, técnico ejecutor, saldo antes y saldo después.
- [ ] Las acciones posteriores sobre el documento (firma, descarga, visualización) NO descuentan créditos del saldo, pero sí incrementan el contador de consultas del tenant.
- [ ] El sistema bloquea el envío de la forma si el saldo de créditos del tenant es 0, antes de intentar generar el PDF.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-19 — Bloqueo al agotar créditos**

> **Como** Cliente sin créditos disponibles que intenta ejecutar una utilización, **quiero** ver un aviso claro con opción de comprar más créditos inmediatamente, **para** no quedar bloqueado sin entender por qué.

**Criterios de Aceptación:**
- [ ] Al intentar ejecutar una utilización con saldo 0, la acción se bloquea antes de procesarse (no consume y luego falla).
- [ ] El sistema muestra un modal con: "Sin créditos disponibles", descripción del paquete más recomendado según el historial de uso y botón "Comprar créditos ahora".
- [ ] El flujo de compra desde el modal es el mismo que US-16, sin salir del contexto actual.
- [ ] Si el usuario no tiene permisos para comprar, el modal indica que debe contactar al administrador de la cuenta.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-20 — Comprar créditos adicionales (Top-up)**

> **Como** Cliente cuyo saldo está bajo pero su paquete anual aún no vence, **quiero** comprar créditos adicionales que se sumen a mi saldo actual, **para** continuar operando sin esperar al próximo año.

**Criterios de Aceptación:**
- [ ] El Cliente puede iniciar una compra de top-up desde el panel de licencias o desde el modal de bloqueo (US-19).
- [ ] El sistema muestra claramente que los créditos del top-up vencen en la misma fecha que el paquete anual activo (no se extienden 12 meses desde la compra).
- [ ] Si el top-up es de modalidad anual, el sistema entrega automáticamente **4 créditos bonus** junto con los créditos del paquete adquirido (RN-C11).
- [ ] Los créditos del top-up (y los 4 bonus si aplica) quedan disponibles en menos de 60 segundos tras el pago.
- [ ] El saldo total visible es la suma del saldo previo más los créditos del top-up más los créditos bonus.
- [ ] El Cliente recibe correo con: créditos añadidos, créditos bonus (si aplica), nuevo saldo total y fecha de vencimiento del paquete activo.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-21 — Recibir alertas de saldo bajo**

> **Como** Cliente, **quiero** recibir notificaciones automáticas cuando mi saldo de créditos llegue a umbrales críticos, **para** poder comprar más créditos antes de que mi operación se vea interrumpida.

**Criterios de Aceptación:**
- [ ] El sistema envía notificación (in-app + correo) cuando el saldo llega al 20% del paquete original.
- [ ] El sistema envía segunda notificación cuando el saldo llega al 10% del paquete original.
- [ ] El sistema envía notificación inmediata cuando el saldo llega a 0.
- [ ] Cada notificación incluye: saldo actual, consumo promedio diario de los últimos 30 días, estimación de días restantes antes de agotar y enlace directo para comprar más créditos.
- [ ] El Cliente puede configurar umbrales personalizados adicionales (ej: notificar también al llegar a 50 créditos).

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-22 — Ver historial de consumo de créditos**

> **Como** Cliente, **quiero** revisar un historial detallado de cómo se han consumido mis créditos, **para** entender qué acciones generan más gasto y planificar mejor el paquete del próximo año.

**Criterios de Aceptación:**
- [ ] El historial muestra cada consumo con: fecha/hora, tipo de acción (utilización), usuario que la ejecutó y saldo después del consumo.
- [ ] El historial es filtrable por: rango de fechas, usuario y tipo de acción.
- [ ] El sistema muestra un resumen agrupado por tipo de acción (ranking de utilizaciones más frecuentes).
- [ ] El historial es exportable a CSV o Excel.
- [ ] Se muestra la proyección de agotamiento de créditos basada en el ritmo de consumo actual (ej: "A este ritmo, tus créditos se agotarán en X días").

**Prioridad:** Media | **Estimación:** 5 pts

---

**US-29 — Ver panel de contadores de consultas del tenant**

> **Como** administrador del tenant, **quiero** ver en tiempo real el contador global de consultas y el desglose por tipo de reporte, junto al umbral incluido en mis créditos, **para** anticiparme al excedente y tomar decisiones antes de que se consuman créditos adicionales.

**Criterios de Aceptación:**
- [ ] El panel muestra: consultas totales acumuladas (contador global), umbral de consultas incluidas (`créditos_adquiridos × 100`) y porcentaje de uso (`global / umbral × 100`).
- [ ] El panel muestra una tabla desglosada con el contador de consultas por tipo de reporte, ordenada de mayor a menor consumo.
- [ ] Si el contador global supera el umbral, el panel muestra: créditos adicionales ya consumidos por excedente y consultas de excedente acumuladas.
- [ ] Cuando el uso de consultas supera el 80% del umbral, el contador global se resalta en amarillo con aviso: "Te quedan X consultas antes de entrar en excedente".
- [ ] Cuando supera el 100%, el contador se resalta en rojo y muestra: "En excedente: X créditos adicionales consumidos".
- [ ] Los datos del panel se actualizan con un máximo de 5 minutos de delay respecto a los eventos reales.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-30 — Recibir alerta al acercarse al umbral de consultas**

> **Como** administrador del tenant, **quiero** recibir una notificación automática cuando mis consultas lleguen al 80% y al 100% del umbral incluido, **para** no ser sorprendido con un cargo de crédito por excedente.

**Criterios de Aceptación:**
- [ ] El sistema envía notificación in-app y correo cuando el contador global alcanza el 80% del umbral.
- [ ] La notificación incluye: consultas acumuladas, umbral total, consultas restantes antes de excedente y enlace al panel de contadores.
- [ ] El sistema envía segunda notificación al llegar al 100% del umbral: "Has superado el umbral incluido. A partir de ahora, cada 100 consultas consumirán 1 crédito adicional".
- [ ] Las notificaciones se envían al correo del administrador de la cuenta del tenant.
- [ ] El administrador puede configurar un umbral de alerta personalizado adicional (ej: avisar también al 60%).

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-31 — Cobro automático de crédito por excedente de consultas**

> **Como** sistema de licencias, **debo** cobrar automáticamente 1 crédito del saldo del tenant cada vez que se acumulen 100 consultas de excedente, **para** garantizar que el uso por encima del umbral incluido se facture correctamente.

**Criterios de Aceptación:**
- [ ] En el momento en que la consulta número `(umbral + 1)` ocurra, el sistema descuenta 1 crédito del saldo del tenant.
- [ ] Cada bloque completo de 100 consultas de excedente descuenta 1 crédito; las consultas parciales dentro del bloque en curso no se cobran hasta completar las 100.
- [ ] El evento de cobro por excedente queda registrado en el historial de créditos con: timestamp, número de consulta que activó el cobro, tipo de evento ("excedente_consultas") y saldo resultante.
- [ ] Si el saldo del tenant es 0 cuando ocurre el cobro por excedente, el sistema registra una deuda y notifica al administrador; las consultas no se bloquean por esta causa.
- [ ] El panel de contadores refleja en tiempo real tanto el excedente acumulado como los créditos consumidos por este concepto.

**Prioridad:** Alta | **Estimación:** 8 pts

---

**US-23 — Vencimiento de créditos no consumidos**

> **Como** Cliente cuyo paquete anual está por vencer con créditos restantes, **quiero** recibir aviso con suficiente anticipación, **para** decidir si acelero su uso antes de que caduquen.

**Criterios de Aceptación:**
- [ ] El sistema envía aviso 30 días antes del vencimiento si hay créditos disponibles: "Te quedan X créditos que vencen el [fecha]. No se transfieren al siguiente período."
- [ ] El sistema envía aviso 7 días antes del vencimiento con el saldo restante y el consumo promedio diario necesario para usarlos todos.
- [ ] Al vencer el período, el saldo de créditos se pone a 0 automáticamente y el log registra los créditos caducados.
- [ ] El sistema no realiza reembolso por créditos caducados; la política es visible en el aviso.
- [ ] Si el Cliente tiene renovación automática del paquete activa, el nuevo paquete se activa al día siguiente del vencimiento con saldo reiniciado.

**Prioridad:** Alta | **Estimación:** 5 pts

---

### ÉPICA 4 — Cancelación y Retención de Datos

> **Como** Cliente, **quiero** poder cancelar mi suscripción de forma clara y saber exactamente qué pasará con mis datos, **para** tomar la decisión informado y sin sorpresas.

---

**US-14 — Cancelar suscripción completa**

> **Como** Cliente que ha decidido no continuar usando el CRM, **quiero** cancelar mi suscripción desde el panel, **para** que no se me realicen cargos futuros.

**Criterios de Aceptación:**
- [ ] La opción de cancelar suscripción está visible pero no prominente (no en el camino crítico de navegación).
- [ ] Al iniciar la cancelación, el sistema muestra: fecha efectiva del fin de acceso, política de retención de datos (90 días) y opción de exportar datos antes de cancelar.
- [ ] El sistema solicita confirmación explícita con frase de verificación (ej: escribir "CANCELAR").
- [ ] No se generan cargos adicionales tras la cancelación; el acceso continúa hasta el final del período pagado.
- [ ] El Cliente recibe confirmación por correo con: fecha de fin de acceso, fecha de purge de datos y enlace para reactivar dentro del período de retención.
- [ ] Los ciclos anuales no generan reembolso por el tiempo no consumido.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-15 — Reactivar cuenta (por cancelación o por mora)**

> **Como** Cliente con cuenta cancelada o suspendida por mora, **quiero** reactivar mi acceso al CRM, **para** retomar la operación recuperando mis datos sin empezar desde cero.

**Criterios de Aceptación:**

*Reactivación tras cancelación voluntaria (dentro de los 90 días de retención):*
- [ ] El sistema permite la reactivación de cuentas canceladas durante los 90 días de retención de datos.
- [ ] Al reactivar, el Cliente puede seleccionar los módulos que desea contratar (no necesariamente los mismos de antes).
- [ ] Todos los datos previos (clientes, historial, configuraciones) están disponibles inmediatamente tras la reactivación.
- [ ] Pasados los 90 días, la reactivación crea una cuenta nueva sin datos históricos.

*Reactivación tras suspensión por mora (Reactivación por Mora):*
- [ ] El panel muestra al Cliente un resumen detallado del adeudo: cuotas vencidas desglosadas por período + Cuotas de Almacenamiento acumuladas + **total a pagar para reactivar**.
- [ ] La Cuota de Almacenamiento mostrada incluye el desglose: número de reportes almacenados, tarifa aplicada y período de cómputo.
- [ ] El sistema **no permite la reactivación parcial**; el pago debe cubrir la totalidad del adeudo (cuotas vencidas + almacenamiento acumulado).
- [ ] Tras el pago completo, el servicio se restaura en menos de 60 segundos.
- [ ] El sistema emite una factura única consolidada de reactivación que detalla los conceptos pagados.

*Ambos escenarios:*
- [ ] El Cliente recibe correo de confirmación de reactivación con: módulos activos, nueva fecha de vencimiento y recordatorio de la política de descuentos por puntualidad.

**Prioridad:** Media | **Estimación:** 8 pts

---

### ÉPICA 8 — Módulo Administrador (Contrato Anual con Cuotas Mensuales)

> **Como** Supervisor o Coordinador de equipo, **quiero** acceder al Módulo Administrador del Mikel CRM bajo un contrato anual de 200 €/mes, **para** tener visibilidad y control total de mi equipo técnico, los tickets en curso y los documentos pendientes de aprobación.

---

**US-32 — Contratar el Módulo Administrador (CACM)**

> **Como** administrador del tenant, **quiero** contratar el Módulo Administrador con un compromiso de 12 meses a 200 €/mes, **para** activar las funcionalidades de supervisión para mi coordinador de equipo.

**Criterios de Aceptación:**
- [ ] El sistema presenta el Módulo Administrador con: descripción funcional, lista de funcionalidades incluidas, cuota mensual (200 €), duración del compromiso (12 meses) y coste total (2.400 €).
- [ ] Se muestra explícitamente el carácter de permanencia anual: "Este módulo requiere compromiso de 12 meses. La primera cuota se cobra hoy; las siguientes el mismo día de cada mes."
- [ ] El flujo de contratación tiene máximo 3 pasos: (1) Revisar condiciones, (2) Confirmar datos de pago, (3) Confirmar contrato.
- [ ] Tras el primer pago exitoso (200 €), el acceso al Módulo Administrador queda activo en menos de 60 segundos.
- [ ] El Cliente recibe correo con: fecha de inicio, fecha de fin del contrato, cuota mensual, total comprometido, y resumen de funcionalidades activadas.
- [ ] Se emite factura por el primer pago de forma inmediata.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-33 — Gestionar el tablero en tiempo real del equipo**

> **Como** Supervisor, **quiero** ver en un tablero centralizado el estado de todo mi equipo en tiempo real, **para** identificar cuellos de botella y redistribuir trabajo sin necesidad de consultar a cada técnico.

**Criterios de Aceptación:**
- [ ] El tablero muestra: técnicos activos/inactivos, número de tickets abiertos por técnico, documentos generados hoy, documentos pendientes de aprobación y alertas de tickets vencidos.
- [ ] Los datos del tablero se actualizan en tiempo real (máximo 30 segundos de delay).
- [ ] El Supervisor puede filtrar el tablero por técnico, fecha y tipo de servicio.
- [ ] El tablero es accesible solo para usuarios con rol Supervisor/Coordinador en el tenant.
- [ ] El tablero no es accesible si el Módulo Administrador está suspendido o el contrato ha vencido.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-34 — Gestionar tickets solicitud→entrega**

> **Como** Supervisor, **quiero** gestionar el ciclo de vida completo de los tickets de servicio desde la solicitud hasta la entrega, **para** asegurar que cada trabajo tiene un responsable, un plazo y un estado visible.

**Criterios de Aceptación:**
- [ ] El Supervisor puede crear un ticket indicando: cliente, tipo de servicio, técnico asignado, prioridad y fecha límite.
- [ ] El ticket atraviesa los estados: `Solicitado → Asignado → En progreso → Pendiente aprobación → Aprobado → Entregado → Cerrado`.
- [ ] El técnico asignado recibe notificación (in-app y correo) al ser asignado un ticket.
- [ ] El Supervisor puede reasignar un ticket a otro técnico en cualquier estado anterior a "Aprobado".
- [ ] El sistema registra con timestamp cada cambio de estado y el usuario que lo realizó.
- [ ] El Supervisor puede ver el historial completo de estados de cualquier ticket.

**Prioridad:** Alta | **Estimación:** 8 pts

---

**US-35 — Aprobar documentos con eFirma de Supervisor**

> **Como** Supervisor, **quiero** revisar y firmar electrónicamente los documentos generados por mi equipo técnico antes de que lleguen al cliente final, **para** asegurar la calidad y trazabilidad de cada entrega.

**Criterios de Aceptación:**
- [ ] El Supervisor recibe notificación cuando un documento generado por un técnico entra en estado "Pendiente de aprobación".
- [ ] El Supervisor puede visualizar el documento completo (PDF) antes de aprobarlo o rechazarlo.
- [ ] Al aprobar, el sistema registra la eFirma del Supervisor con: timestamp, hash del documento firmado e identidad del firmante.
- [ ] La eFirma del Supervisor queda visible en el documento como sello de validación, diferenciada de la firma del técnico.
- [ ] Si el Supervisor rechaza el documento, debe indicar el motivo; el ticket regresa al técnico con el motivo de rechazo visible.
- [ ] Un documento aprobado no puede ser modificado; cualquier cambio requiere generar un nuevo documento (nuevo evento de crédito).
- [ ] El evento de aprobación y eFirma de Supervisor se contabiliza como una consulta en el contador del tenant (no consume crédito adicional).

**Prioridad:** Alta | **Estimación:** 8 pts

---

**US-36 — Consultar historial completo por cliente para auditoría**

> **Como** Supervisor, **quiero** acceder al historial completo de todos los tickets, documentos, firmas y estados asociados a un cliente específico, **para** responder a auditorías internas o externas con información trazable y ordenada.

**Criterios de Aceptación:**
- [ ] El historial por cliente muestra en orden cronológico: tickets creados, estados y fechas de cada transición, documentos generados (con enlace al PDF), firmas emitidas (técnico y supervisor) y técnicos involucrados.
- [ ] El historial es filtrable por rango de fechas, tipo de documento y técnico.
- [ ] El historial es exportable en PDF o Excel.
- [ ] Cada acceso al historial por parte del Supervisor se contabiliza como una consulta en el contador del tenant.
- [ ] El historial incluye marca de tiempo de cada evento y no puede ser editado ni eliminado desde el panel.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-37 — Ver estado del contrato anual del Módulo Administrador**

> **Como** administrador del tenant, **quiero** ver el estado de mi contrato anual del Módulo Administrador con cuotas pagadas, cuotas pendientes y fecha de fin, **para** planificar la renovación o baja con anticipación.

**Criterios de Aceptación:**
- [ ] El panel de licencias muestra para el Módulo Administrador: fecha de inicio del contrato, fecha de fin, cuotas pagadas (N/12), cuotas pendientes, próxima fecha de cobro y monto.
- [ ] Se muestra una barra de progreso visual del avance del contrato (ej: mes 5 de 12).
- [ ] A partir del mes 10, el panel muestra un aviso de vencimiento próximo con las opciones disponibles: renovar, continuar o no renovar.
- [ ] El sistema envía notificación 30 y 7 días antes del fin del contrato al administrador del tenant.
- [ ] El administrador puede iniciar el proceso de baja anticipada desde este panel *(aplican condiciones pendientes de definición — RN-ADM-05)*.

**Prioridad:** Alta | **Estimación:** 3 pts

---

### ÉPICA 9 — Política de Pagos, Descuentos y Almacenamiento por Mora

> **Como** Cliente, **quiero** entender claramente las ventajas de pagar a tiempo y las consecuencias de no hacerlo, **para** tomar decisiones financieras informadas y evitar interrupciones del servicio.

---

**US-38 — Ver descuento disponible antes de pagar**

> **Como** Cliente con una factura pendiente, **quiero** ver claramente qué descuento obtengo si pago hoy o antes del vencimiento, **para** decidir si adelanto el pago y aprovechar el beneficio.

**Criterios de Aceptación:**
- [ ] El panel de licencias muestra, para cada factura pendiente: monto original, **descuento aplicable si paga hoy** (10 % si es antes del límite, 3 % si es el día límite, 0 % si es el día +1) y **monto final a pagar hoy**.
- [ ] El indicador de descuento se actualiza en tiempo real según la fecha actual: la mañana del día límite muestra el 3 %, el día siguiente muestra precio normal.
- [ ] Si ya es día +2 o posterior, el panel no muestra descuento sino el adeudo de mora con las Cuotas de Almacenamiento acumuladas.
- [ ] Cada correo de alerta de vencimiento incluye el descuento vigente en la fecha de envío del correo.

**Prioridad:** Alta | **Estimación:** 3 pts

---

**US-39 — Ver estado de mora y adeudo de almacenamiento**

> **Como** Cliente con servicio suspendido, **quiero** ver en mi panel exactamente cuánto debo pagar para reactivar el servicio, desglosado por concepto, **para** no tener sorpresas al momento del pago de reactivación.

**Criterios de Aceptación:**
- [ ] El panel muestra un banner prominente de "Servicio Suspendido" con: fecha de inicio de la suspensión y días transcurridos.
- [ ] Se muestra el desglose completo del adeudo:
  - Cuotas de servicio vencidas (una línea por cada período adeudado).
  - Cuotas de Almacenamiento por cada mes de suspensión (con el número de reportes y tarifa aplicada de cada mes).
  - **Total a pagar para reactivar** (suma de ambos conceptos).
- [ ] El número de reportes almacenados que determina la Cuota de Almacenamiento es visible y auditable por el Cliente.
- [ ] Existe un botón "Pagar y reactivar ahora" que inicia el flujo de pago por el monto total (sin posibilidad de pago parcial).

**Prioridad:** Alta | **Estimación:** 5 pts

---

### ÉPICA 10 — Portal de Contratación, Dashboard Tenant y Umbrales de Promoción

> **Como** equipo de producto, **queremos** ofrecer una experiencia de autoservicio guiada tanto a prospectos como a clientes activos, **para** reducir fricción en la contratación inicial y en la gestión continua del contrato, mientras el sistema detecta automáticamente los momentos óptimos para ofrecer condiciones preferenciales.

---

**US-40 — Pantalla de contratación inicial para nuevo prospecto**

> **Como** prospecto (empresa que evalúa Mikel CRM), **quiero** explorar los módulos disponibles, seleccionar los que necesito y ver en tiempo real el costo anual y la cuota mensual estimada, **para** tomar una decisión de compra informada sin necesidad de contactar a un representante de ventas.

**Criterios de Aceptación:**
- [ ] La pantalla es pública (sin autenticación). Se accede desde la web de marketing o desde un enlace de onboarding.
- [ ] Se muestran todos los módulos disponibles como tarjetas con checkbox: nombre, descripción corta (máx. 2 líneas), precio anual y equivalente mensual.
- [ ] El módulo **CRM Base** aparece marcado por defecto y no puede desmarcarse (es requerido).
- [ ] Los paquetes de créditos (100 / 500 + bonus) se muestran en una sección separada como complementos opcionales.
- [ ] La calculadora se actualiza en tiempo real al seleccionar/deseleccionar módulos: muestra subtotal por módulo, total anual y cuota mensual estimada.
- [ ] Se indica claramente la política de descuentos por pago anticipado: 10 % (antes del límite) / 3 % (en fecha límite) / tarifa normal (día +1).
- [ ] El botón **"Ver resumen de servicios"** genera un documento de texto (visible en pantalla, copiable) con la descripción completa de cada servicio seleccionado y el desglose de precios.
- [ ] El botón **"Solicitar contrato"** redirige al flujo de alta (o abre un formulario de contacto comercial), transmitiendo la selección de módulos como contexto.
- [ ] La pantalla es responsive y funciona en móvil y tablet.

**Prioridad:** Alta | **Estimación:** 5 pts

---

**US-41 — Dashboard de tenant autenticado: estado de contrato y créditos**

> **Como** administrador de una empresa cliente (tenant activo), **quiero** ver en una sola pantalla el estado completo de mi contrato, los módulos contratados, el consumo de créditos del mes en curso, el saldo disponible y las opciones de ampliación, **para** gestionar mi cuenta sin depender del equipo de soporte.

**Criterios de Aceptación:**
- [ ] La pantalla requiere autenticación. Solo el rol ADMIN_CUENTA tiene acceso completo; SUPERVISOR ve en modo lectura.
- [ ] **Sección Contrato:** muestra código de contrato, fecha de inicio, fecha de vencimiento, días restantes, plan activo y estado (ACTIVO / SUSPENDIDO / EN RENOVACIÓN).
- [ ] **Sección Módulos contratados:** lista de módulos activos con precio y fecha de activación. Los módulos no contratados se muestran en gris con botón "Añadir módulo".
- [ ] **Sección Créditos:**
  - Créditos totales contratados en el plan anual vigente.
  - Créditos consumidos en el mes en curso.
  - Créditos totales consumidos desde el inicio del período anual.
  - Créditos restantes (saldo disponible).
  - Barra de progreso visual que cambia de color al superar el 80 % (amarillo) y 95 % (rojo) del saldo total.
- [ ] **Sección Cuota próxima:** monto a pagar, fecha límite, descuento vigente si paga hoy.
- [ ] **Historial de consumo mensual:** tabla de los últimos 6 meses con créditos consumidos por mes.
- [ ] Botones de acción: "Comprar créditos adicionales", "Añadir módulo", "Ver historial de facturas".
- [ ] Si el contrato está dentro de la Ventana de Renovación (≤ 30 días) o el saldo de créditos está por debajo del umbral crítico (≤ 5 % del total), se activa automáticamente el banner de promoción definido en US-42.

**Prioridad:** Alta | **Estimación:** 8 pts

---

**US-42 — Configuración de umbrales y promociones de renovación y créditos**

> **Como** administrador de la plataforma Mikel CRM (rol interno), **quiero** configurar los umbrales que activan avisos y las tarifas preferenciales que se ofrecen automáticamente en esos momentos, **para** maximizar la retención de clientes y las ventas de créditos adicionales sin intervención manual.

**Criterios de Aceptación:**

**Umbrales de renovación de contrato:**
- [ ] Configurable: número de días antes del vencimiento para activar cada tipo de notificación (valores por defecto: -30, -7, -1 días).
- [ ] Configurable: porcentaje de descuento preferencial sobre la tarifa de renovación (€99) cuando se renueva dentro de la Ventana de Renovación (valor por defecto: 15 %).
- [ ] Configurable: período de vigencia de la tarifa preferencial (valor por defecto: los 30 días de la Ventana de Renovación).
- [ ] El sistema aplica automáticamente el descuento configurado en el checkout de renovación si el tenant renueva dentro del período definido.

**Umbrales de créditos:**
- [ ] Configurable: porcentaje de saldo consumido que activa la alerta amarilla (valor por defecto: 80 %).
- [ ] Configurable: porcentaje de saldo consumido que activa la alerta roja y el banner de compra urgente (valor por defecto: 95 %).
- [ ] Configurable: descuento preferencial para compra de créditos cuando el tenant está dentro de la Ventana de Renovación (valor por defecto: 10 % sobre precio de lista).
- [ ] El banner de promoción en el dashboard muestra el descuento vigente calculado por el sistema.

**Promociones combinadas:**
- [ ] Si el tenant está simultáneamente en Ventana de Renovación **y** ha superado el umbral crítico de créditos, el sistema muestra una oferta combinada: renovación + paquete de créditos con la tarifa preferencial de ambos.
- [ ] Todas las configuraciones de umbrales y descuentos tienen historial de cambios (quién modificó, cuándo, valor anterior / nuevo).
- [ ] Los cambios de configuración surten efecto en los nuevos cálculos; no retroactivos a banners ya mostrados.

**Reglas de negocio asociadas (RN-UMP):**

| ID | Regla |
|----|-------|
| RN-UMP-01 | La tarifa preferencial de renovación aplica solo si el pago se completa dentro de la Ventana de Renovación (-30 a 0 días del vencimiento) |
| RN-UMP-02 | El descuento de renovación no es acumulable con el descuento por pago anticipado de cuota mensual |
| RN-UMP-03 | El descuento de créditos en Ventana de Renovación aplica únicamente a paquetes anuales (≥ 500 créditos) |
| RN-UMP-04 | Si el tenant ya renovó el contrato, la Ventana de Renovación se reinicia al nuevo ciclo y los descuentos dejan de aplicar |
| RN-UMP-05 | Un tenant con servicio suspendido (mora) no puede acceder a tarifas preferenciales hasta saldar el adeudo completo |

**Prioridad:** Alta | **Estimación:** 5 pts

---

## Matriz de Trazabilidad

| Historia | Requerimientos relacionados | Épica | Prioridad | Estimación |
|----------|-----------------------------|-------|-----------|-----------|
| US-01 | RF-01.2 | Activación | Alta | 3 pts |
| US-02 | RF-01.1, RF-03.1, RF-03.2, RF-03.3 | Activación | Alta | 5 pts |
| US-03 | RF-03.4, RF-03.5 | Activación | Media | 5 pts |
| US-04 | RF-01.5, RF-01.6 | Activación | Media | 3 pts |
| US-05 | RF-05.1 | Suscripción | Alta | 3 pts |
| US-06 | RF-01.3, RF-06.3, RF-06.4 | Suscripción | Alta | 5 pts |
| US-07 | RF-01.4, RF-06.2, RF-02.7 | Suscripción | Media | 5 pts |
| ~~US-08~~ | ~~RF-02.2, RF-02.4~~ | ~~Suscripción~~ | ~~Alta~~ | ~~5 pts — ELIMINADA~~ |
| US-09 | RF-04.1 | Renovación | Alta | 3 pts |
| US-10 | RF-04.2, RNF-03 | Renovación | Alta | 3 pts |
| US-11 | RF-04.3, RF-04.4, RF-04.5, RF-04.6, RF-14.1, RF-14.2, RF-14.3, RF-14.4, RF-14.5, RF-14.6, RF-14.7, RF-14.8, RF-14.9, RF-14.10, RF-14.11, RF-14.12, RN-PAG-01, RN-PAG-02, RN-PAG-04 | Renovación/Pagos | Alta | 13 pts |
| US-12 | RF-02.5, RF-02.6, RF-02.7, RF-02.8 | Renovación | Alta | 3 pts |
| US-13 | RF-04.7, RF-05.2 | Renovación | Media | 5 pts |
| US-14 | RF-05.4, RF-05.5, RF-06.6 | Cancelación | Alta | 5 pts |
| US-15 | RF-05.5 | Cancelación | Media | 5 pts |
| US-16 | RF-07.4, RF-07.14, RF-07.15, RF-07.16, RN-C11, RN-C12, RN-C13 | Créditos | Alta | 5 pts |
| US-17 | RF-07.6, RF-08.10 | Créditos | Alta | 3 pts |
| US-18 | RF-07.1, RF-07.2, RF-07.3, RN-C01, RN-C02 | Créditos | Alta | 5 pts |
| US-19 | RF-07.5 | Créditos | Alta | 3 pts |
| US-20 | RF-07.8, RF-07.9, RF-07.14, RN-C04, RN-C11, RN-C12 | Créditos | Alta | 5 pts |
| US-21 | RF-07.7, RF-08.11 | Créditos | Alta | 3 pts |
| US-22 | RF-07.12, RF-07.13 | Créditos | Media | 5 pts |
| US-23 | RF-07.10, RN-C10 | Créditos | Alta | 5 pts |
| US-24 | RF-09.2, RF-09.3, RF-10.1 | Apertura | Alta | 3 pts |
| US-25 | RF-09.1, RF-09.4, RF-09.6, RF-09.14, RF-10.2 | Apertura | Alta | 3 pts |
| US-26 | RF-09.1, RF-09.4, RF-09.7, RF-09.8, RF-09.9, RF-09.10, RF-09.11 | Apertura | Alta | 8 pts |
| US-27 | RF-09.12, RF-09.13 | Apertura | Media | 5 pts |
| US-28 | RF-09.15, RF-10.5 | Apertura | Media | 5 pts |
| US-29 | RF-08.1, RF-08.2, RF-08.10, RF-08.11 | Contadores | Alta | 5 pts |
| US-30 | RF-08.11, RN-C06 | Contadores | Alta | 3 pts |
| US-31 | RF-08.5, RF-08.6, RF-08.7, RN-C03, RN-C05 | Contadores | Alta | 8 pts |

| US-32 | RF-12.1, RF-12.2, RF-12.3, RF-12.4, RF-12.5 | Módulo Admin | Alta | 5 pts |
| US-33 | RF-11 F-ADM-01 | Módulo Admin | Alta | 5 pts |
| US-34 | RF-11 F-ADM-02 | Módulo Admin | Alta | 8 pts |
| US-35 | RF-11 F-ADM-03, F-ADM-04, RN-ADM-08 | Módulo Admin | Alta | 8 pts |
| US-36 | RF-11 F-ADM-05 | Módulo Admin | Alta | 5 pts |
| US-37 | RF-12.4, RF-12.5, RF-02.4, RF-02.5, RF-04.8, RF-04.9 | Módulo Admin | Alta | 3 pts |
| US-38 | RF-14.1, RF-14.2, RF-14.3, RF-14.8, RN-PAG-01, RN-PAG-07, RN-PAG-08 | Pagos | Alta | 3 pts |
| US-39 | RF-14.4, RF-14.5, RF-14.6, RF-14.7, RF-14.8, RF-14.9, RF-14.10, RF-14.11, RF-14.12, RN-PAG-03, RN-PAG-05, RN-PAG-06 | Pagos | Alta | 5 pts |
| US-40 | RF-16.1 (nuevo), RF-09.1, RF-09.4, RF-14.1, RF-14.8 | Portal Contratación | Alta | 5 pts |
| US-41 | RF-16.2 (nuevo), RF-07.6, RF-07.12, RF-07.13, RF-08.10, RF-08.11, RF-04.1, RF-04.2 | Portal Contratación | Alta | 8 pts |
| US-42 | RF-16.3 (nuevo), RN-UMP-01..05 | Portal Contratación | Alta | 5 pts |

**Total estimado del backlog: 203 story points** *(185 pts existentes + 18 pts Épica 10)*

---

## Resumen por Prioridad

| Prioridad | # Historias activas | Story Points |
|-----------|---------------------|-------------|
| Alta | 39 | 178 pts |
| Media | 4 | 25 pts |
| **Total activo** | **43** | **203 pts** |

## Resumen por Épica

| Épica | # Historias activas | Story Points |
|-------|---------------------|-------------|
| 1 — Activación de Módulos | 4 | 16 pts |
| 2 — Gestión de Contratos Anuales | 3 | 13 pts |
| 3 — Cuotas Mensuales, Mora y Renovación | 5 | 27 pts |
| 4 — Cancelación y Retención | 2 | 13 pts |
| 5 — Licencia por Créditos Anuales | 8 | 34 pts |
| 6 — Apertura de Contrato | 5 | 24 pts |
| 7 — Contadores de Consultas por Tenant | 3 | 16 pts |
| 8 — Módulo Administrador (Contrato Anual) | 6 | 34 pts |
| 9 — Política de Pagos y Almacenamiento | 2 | 8 pts |
| 10 — Portal Contratación, Dashboard y Umbrales | 3 | 18 pts |
| **Total activo** | **41** | **203 pts** |

---

> **Próximos pasos sugeridos:**
> 1. Validar este backlog con el equipo de producto y stakeholders.
> 2. Confirmar precio definitivo de apertura (400 € / 1.400 €) y política de reembolso con el área comercial.
> 3. Definir qué acciones específicas dentro del CRM se catalogan como "utilización" (1 crédito) — crítico para US-18.
> 4. Definir el catálogo completo de tipos de reporte para el sistema de contadores por tipo (RF-08.2).
> 5. Resolver las 3 decisiones comerciales pendientes del Módulo Administrador (RN-ADM-05, 06, 07): penalización por baja anticipada, créditos incluidos y precio de renovación.
> 6. Definir flujo de estados completo de tickets del Módulo Administrador y sus notificaciones (US-34).
> 7. Elaborar el SDD técnico (ver `SDD_TEMPLATE.md`) por épica, comenzando por Épica 6 (Apertura) → Épica 7 (Contadores) → Épica 8 (Módulo Admin).

> **⚠️ Decisiones comerciales bloqueantes (pendientes):**
> - `RN-ADM-05` — Política de penalización por baja anticipada del Módulo Administrador
> - `RN-ADM-06` — Si el Módulo Administrador incluye créditos o solo acceso funcional
> - `RN-ADM-07` — Precio y modalidad de renovación tras los 12 meses del CACM
