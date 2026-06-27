# Documento de Requerimientos — Portal de Contratación Frontend

## Introducción

Portal web frontend para la plataforma Mikel CRM que expone dos experiencias distintas:

1. **Pantalla pública de prospecto (US-40):** Calculadora interactiva de servicios para visitantes no autenticados. Permite seleccionar módulos, visualizar precios en tiempo real, generar un resumen de servicios y solicitar un contrato formal.

2. **Dashboard de tenant autenticado (US-41):** Panel de gestión para clientes activos que muestra el estado del contrato, módulos contratados, saldo de créditos, cuota próxima y banners promocionales automáticos. Consume la Query API del `license-service` existente.

El backend (`license-service`) ya está implementado y disponible en `http://localhost:8080`. El frontend consume exclusivamente los endpoints de lectura (Query API).

## Glosario

- **Portal**: Aplicación web frontend que implementa las pantallas públicas y autenticadas del Portal de Contratación
- **Prospecto**: Visitante no autenticado que explora la oferta de módulos y precios en la pantalla pública
- **Calculadora**: Componente interactivo que calcula subtotal por módulo, total anual y cuota mensual estimada en tiempo real
- **Módulo_Disponible**: Cada uno de los módulos del Mikel CRM que puede ser contratado (CRM Base, Reportes, Tickets, Administrador, etc.)
- **CRM_Base**: Módulo obligatorio que siempre debe estar seleccionado; no puede desmarcarse
- **Paquete_Creditos**: Complemento opcional de créditos anuales (100 o 500 + bonus de 4) disponible para contratar junto con los módulos
- **Resumen_Servicios**: Documento de texto generado en pantalla que detalla los módulos seleccionados, precios y condiciones; copiable al portapapeles
- **Dashboard**: Panel principal del tenant autenticado con información de contrato, módulos, créditos y cuota próxima
- **Tenant_Autenticado**: Usuario con sesión activa y rol válido (ADMIN_CUENTA o SUPERVISOR) que accede al Dashboard
- **ADMIN_CUENTA**: Rol con acceso completo al Dashboard, incluyendo acciones de compra y gestión
- **SUPERVISOR**: Rol con acceso de solo lectura al Dashboard
- **Ventana_Renovación**: Período de 30 días previos al vencimiento del contrato durante el cual se muestra un banner promocional
- **Barra_Progreso_Créditos**: Indicador visual del consumo de créditos con semáforo de colores (verde → amarillo → rojo)
- **Banner_Promocional**: Mensaje automático que aparece en el Dashboard cuando se cumplen condiciones específicas (renovación próxima o créditos bajos)
- **License_Service_API**: Backend REST existente que provee los datos del tenant (endpoints GET /access, /contracts, /credits, /reactivation-summary)
- **Política_Descuentos**: Regla visible en la pantalla pública: 10% si paga antes del límite, 3% en fecha límite, 0% después

## Requerimientos

### Requerimiento 1: Selección de módulos con tarjetas interactivas

**User Story:** Como prospecto, quiero ver tarjetas con checkbox por cada módulo disponible, para poder seleccionar los servicios que me interesan de forma visual.

#### Criterios de Aceptación

1. THE Portal SHALL mostrar una tarjeta por cada módulo disponible con nombre, descripción breve y precio mensual
2. WHEN el prospecto hace clic en una tarjeta o su checkbox, THE Portal SHALL alternar el estado de selección del módulo
3. THE Portal SHALL mostrar la tarjeta del módulo CRM_Base marcada por defecto y con el checkbox deshabilitado para impedir su desmarcación
4. WHEN se carga la pantalla pública, THE Portal SHALL preseleccionar el módulo CRM_Base como obligatorio
5. THE Portal SHALL distinguir visualmente las tarjetas seleccionadas de las no seleccionadas mediante un indicador de estado (borde, color de fondo o icono)

### Requerimiento 2: Calculadora de precios en tiempo real

**User Story:** Como prospecto, quiero ver una calculadora que actualice automáticamente los totales conforme selecciono módulos, para conocer el costo exacto de mi configuración.

#### Criterios de Aceptación

1. WHEN el prospecto selecciona o deselecciona un módulo, THE Calculadora SHALL recalcular y mostrar el subtotal por módulo seleccionado, el total anual y la cuota mensual estimada en menos de 100 milisegundos
2. THE Calculadora SHALL mostrar el subtotal individual de cada módulo seleccionado en la lista de resumen
3. THE Calculadora SHALL calcular el total anual como la suma de todos los subtotales anuales de módulos seleccionados más el costo de paquetes de créditos seleccionados
4. THE Calculadora SHALL calcular la cuota mensual estimada como el total anual dividido entre 12
5. WHILE no haya módulos adicionales seleccionados más allá de CRM_Base, THE Calculadora SHALL mostrar únicamente el subtotal de CRM_Base como total

### Requerimiento 3: Paquetes de créditos como complementos opcionales

**User Story:** Como prospecto, quiero poder añadir paquetes de créditos a mi selección, para complementar mi contratación de módulos con capacidad de generación de documentos.

#### Criterios de Aceptación

1. THE Portal SHALL mostrar los paquetes de créditos (100 créditos y 500 créditos + bonus de 4) en una sección separada e identificada como "Complementos opcionales"
2. WHEN el prospecto selecciona un paquete de créditos, THE Calculadora SHALL incluir su costo en el total anual y la cuota mensual estimada
3. THE Portal SHALL mostrar junto a cada paquete la cantidad de créditos incluidos, el bonus de 4 créditos y el precio anual del paquete
4. THE Portal SHALL permitir seleccionar un solo paquete de créditos o ninguno

### Requerimiento 4: Política de descuentos visible

**User Story:** Como prospecto, quiero ver claramente la política de descuentos por puntualidad en el pago, para entender los beneficios de pagar antes de la fecha límite.

#### Criterios de Aceptación

1. THE Portal SHALL mostrar la política de descuentos en la pantalla pública con los tres niveles: 10% antes del límite, 3% en fecha límite, 0% después del día de la fecha límite
2. THE Portal SHALL presentar la política de descuentos en un formato visual que permita comparar los tres niveles de forma inmediata (tabla, tarjetas comparativas o similar)
3. THE Portal SHALL mostrar la política de descuentos sin requerir interacción adicional del prospecto (visible directamente en la pantalla, sin necesidad de hacer clic ni desplegar)

### Requerimiento 5: Generación de resumen de servicios

**User Story:** Como prospecto, quiero generar un documento de resumen con mi selección de servicios y precios, para poder revisarlo o compartirlo antes de solicitar el contrato.

#### Criterios de Aceptación

1. WHEN el prospecto hace clic en "Ver resumen de servicios", THE Portal SHALL generar un documento de texto visible en pantalla con los módulos seleccionados, precios unitarios, total anual y cuota mensual estimada
2. THE Portal SHALL incluir en el resumen la política de descuentos aplicable y la fecha de generación
3. THE Portal SHALL proporcionar un botón para copiar el contenido completo del resumen al portapapeles del sistema
4. WHEN el prospecto copia el resumen exitosamente, THE Portal SHALL mostrar una confirmación visual temporal indicando que el texto fue copiado
5. IF la función de copia al portapapeles no es soportada por el navegador, THEN THE Portal SHALL mostrar el texto seleccionado para copia manual

### Requerimiento 6: Solicitud de contrato con transmisión de selección

**User Story:** Como prospecto, quiero solicitar un contrato formal y que mi selección de módulos se transmita automáticamente al flujo de alta, para no tener que repetir mi configuración.

#### Criterios de Aceptación

1. WHEN el prospecto hace clic en "Solicitar contrato", THE Portal SHALL redirigir al flujo de alta transmitiendo la selección de módulos y paquetes de créditos como parámetros
2. THE Portal SHALL validar que al menos un módulo está seleccionado (CRM_Base como mínimo) antes de permitir la solicitud
3. THE Portal SHALL codificar la selección de módulos en la URL o en el estado de navegación de forma que el flujo de alta pueda reconstruirla sin pérdida de datos

### Requerimiento 7: Diseño responsive

**User Story:** Como prospecto, quiero poder navegar la pantalla de contratación desde mi móvil o tablet, para explorar la oferta desde cualquier dispositivo.

#### Criterios de Aceptación

1. THE Portal SHALL adaptar el layout de tarjetas de módulos a una columna en pantallas de menos de 768px de ancho
2. THE Portal SHALL adaptar el layout de tarjetas a dos columnas en pantallas entre 768px y 1024px
3. THE Portal SHALL mantener la calculadora visible o accesible con un solo toque en dispositivos móviles
4. THE Portal SHALL asegurar que todos los botones y áreas interactivas tengan un tamaño mínimo de 44x44 píxeles en dispositivos táctiles
5. THE Portal SHALL renderizar correctamente en los navegadores Chrome, Firefox, Safari y Edge en sus últimas dos versiones estables

### Requerimiento 8: Accesibilidad de la pantalla pública

**User Story:** Como prospecto con necesidades de accesibilidad, quiero que la pantalla pública sea navegable con teclado y compatible con lectores de pantalla, para poder explorar la oferta sin barreras.

#### Criterios de Aceptación

1. THE Portal SHALL permitir la navegación completa por teclado (Tab, Enter, Space) entre tarjetas, checkbox y botones
2. THE Portal SHALL proporcionar etiquetas ARIA descriptivas en todos los elementos interactivos
3. THE Portal SHALL mantener un contraste de color mínimo de 4.5:1 entre texto y fondo según WCAG 2.1 AA
4. WHEN la Calculadora actualiza los totales, THE Portal SHALL anunciar los cambios a lectores de pantalla mediante regiones ARIA live

### Requerimiento 9: Dashboard de contrato del tenant autenticado

**User Story:** Como ADMIN_CUENTA o SUPERVISOR, quiero ver el estado de mi contrato en un panel claro, para conocer mi situación contractual actual de un vistazo.

#### Criterios de Aceptación

1. THE Dashboard SHALL mostrar la sección "Contrato" con: código de contrato, fecha de inicio, fecha de vencimiento, días restantes hasta el vencimiento, y estado del contrato (ACTIVO, SUSPENDIDO o EN RENOVACIÓN)
2. WHEN el estado del contrato es SUSPENDIDO, THE Dashboard SHALL mostrar el estado con un indicador visual de alerta (color rojo o icono de advertencia)
3. WHEN el estado del contrato es EN RENOVACIÓN, THE Dashboard SHALL mostrar el estado con un indicador visual informativo distinto de ACTIVO y SUSPENDIDO
4. THE Dashboard SHALL obtener los datos de contrato consumiendo el endpoint GET /api/v1/tenants/{tenantId}/contracts de la License_Service_API
5. IF la llamada a la License_Service_API falla, THEN THE Dashboard SHALL mostrar un mensaje de error amigable con opción de reintentar

### Requerimiento 10: Sección de módulos contratados

**User Story:** Como ADMIN_CUENTA, quiero ver los módulos que tengo activos y los que podría agregar, para gestionar mi suscripción desde el Dashboard.

#### Criterios de Aceptación

1. THE Dashboard SHALL mostrar la lista de módulos activos con nombre, precio mensual y fecha de activación por cada uno
2. THE Dashboard SHALL mostrar los módulos no contratados en estilo atenuado (gris) con un botón "Añadir módulo" por cada uno
3. WHILE el usuario tiene rol SUPERVISOR, THE Dashboard SHALL ocultar los botones "Añadir módulo" en los módulos no contratados
4. WHEN un ADMIN_CUENTA hace clic en "Añadir módulo", THE Portal SHALL iniciar el flujo de contratación del módulo seleccionado

### Requerimiento 11: Sección de créditos con barra de progreso

**User Story:** Como ADMIN_CUENTA o SUPERVISOR, quiero ver mi saldo de créditos con un indicador visual de consumo, para anticipar cuándo necesitaré comprar más créditos.

#### Criterios de Aceptación

1. THE Dashboard SHALL mostrar en la sección "Créditos": créditos totales contratados, créditos consumidos en el mes en curso, créditos consumidos acumulados desde inicio de contrato, y saldo disponible (restantes)
2. THE Dashboard SHALL mostrar una Barra_Progreso_Créditos que represente el porcentaje de consumo respecto al total contratado
3. WHILE el consumo acumulado es menor o igual al 80% del total, THE Barra_Progreso_Créditos SHALL mostrarse en color verde
4. WHILE el consumo acumulado es mayor al 80% y menor o igual al 95% del total, THE Barra_Progreso_Créditos SHALL mostrarse en color amarillo
5. WHILE el consumo acumulado es mayor al 95% del total, THE Barra_Progreso_Créditos SHALL mostrarse en color rojo
6. THE Dashboard SHALL obtener los datos de créditos consumiendo el endpoint GET /api/v1/tenants/{tenantId}/credits de la License_Service_API

### Requerimiento 12: Sección de cuota próxima

**User Story:** Como ADMIN_CUENTA, quiero ver el monto de mi próxima cuota y el descuento disponible hoy, para motivarme a pagar anticipadamente.

#### Criterios de Aceptación

1. THE Dashboard SHALL mostrar en la sección "Cuota próxima": monto a pagar, fecha límite de pago, y descuento vigente si el tenant paga en el día actual
2. WHEN la fecha actual es anterior a la fecha límite, THE Dashboard SHALL mostrar "10% de descuento disponible si paga hoy"
3. WHEN la fecha actual es igual a la fecha límite, THE Dashboard SHALL mostrar "3% de descuento disponible si paga hoy"
4. WHEN la fecha actual es posterior a la fecha límite, THE Dashboard SHALL mostrar "Sin descuento disponible"
5. THE Dashboard SHALL obtener los datos de cuota y descuento del endpoint GET /api/v1/tenants/{tenantId}/contracts de la License_Service_API

### Requerimiento 13: Historial de consumo mensual

**User Story:** Como ADMIN_CUENTA o SUPERVISOR, quiero ver una tabla con el consumo de créditos de los últimos 6 meses, para analizar tendencias de uso.

#### Criterios de Aceptación

1. THE Dashboard SHALL mostrar una tabla con los últimos 6 meses de consumo de créditos, con columnas: mes, créditos consumidos
2. THE Dashboard SHALL ordenar la tabla del mes más reciente al más antiguo
3. IF no hay datos de consumo para algún mes del período, THEN THE Dashboard SHALL mostrar 0 créditos consumidos para ese mes
4. THE Dashboard SHALL calcular los datos de historial a partir de la información provista por la License_Service_API

### Requerimiento 14: Botones de acción del Dashboard

**User Story:** Como ADMIN_CUENTA, quiero tener acceso rápido a acciones frecuentes como comprar créditos, añadir un módulo o ver facturas, para gestionar mi cuenta sin fricciones.

#### Criterios de Aceptación

1. THE Dashboard SHALL mostrar los botones de acción: "Comprar créditos adicionales", "Añadir módulo" y "Ver historial de facturas"
2. WHILE el usuario tiene rol SUPERVISOR, THE Dashboard SHALL ocultar los botones "Comprar créditos adicionales" y "Añadir módulo"
3. WHILE el usuario tiene rol SUPERVISOR, THE Dashboard SHALL mantener visible el botón "Ver historial de facturas"
4. WHEN un ADMIN_CUENTA hace clic en "Comprar créditos adicionales", THE Portal SHALL navegar al flujo de compra de paquetes de créditos

### Requerimiento 15: Banners promocionales automáticos

**User Story:** Como ADMIN_CUENTA o SUPERVISOR, quiero recibir alertas visuales cuando mi contrato está por vencer o mis créditos se están agotando, para tomar acción preventiva.

#### Criterios de Aceptación

1. WHEN el contrato del tenant está dentro de la Ventana_Renovación (30 días o menos para el vencimiento), THE Dashboard SHALL mostrar un banner promocional de renovación con los días restantes y la opción de renovar
2. WHEN el saldo de créditos del tenant es menor o igual al 5% del total contratado, THE Dashboard SHALL mostrar un banner de alerta de créditos bajos con el saldo actual y la opción de comprar más
3. WHEN ambas condiciones se cumplen simultáneamente, THE Dashboard SHALL mostrar ambos banners, priorizando el de renovación en posición superior
4. THE Dashboard SHALL permitir al usuario cerrar (descartar) un banner promocional durante la sesión activa sin eliminar la condición subyacente

### Requerimiento 16: Autenticación y control de acceso del Dashboard

**User Story:** Como sistema, quiero que el Dashboard requiera autenticación y respete los roles definidos, para proteger la información del tenant y limitar acciones según permisos.

#### Criterios de Aceptación

1. WHEN un usuario no autenticado intenta acceder al Dashboard, THE Portal SHALL redirigir al flujo de autenticación
2. THE Portal SHALL validar que el usuario autenticado tiene rol ADMIN_CUENTA o SUPERVISOR antes de mostrar el Dashboard
3. IF un usuario autenticado no tiene rol ADMIN_CUENTA ni SUPERVISOR, THEN THE Portal SHALL mostrar una pantalla de acceso denegado
4. WHILE el usuario tiene rol SUPERVISOR, THE Dashboard SHALL funcionar en modo solo lectura (sin botones de acción que modifiquen datos)
5. THE Portal SHALL incluir el token JWT del usuario en todas las llamadas a la License_Service_API como cabecera de autorización

### Requerimiento 17: Manejo de errores y estados de carga

**User Story:** Como tenant autenticado, quiero que el Dashboard me informe claramente cuando hay problemas de conectividad o los datos están cargando, para no confundir una carga lenta con un error.

#### Criterios de Aceptación

1. WHILE los datos de una sección están cargándose, THE Dashboard SHALL mostrar un indicador de carga (skeleton o spinner) en lugar de datos vacíos
2. IF una llamada a la License_Service_API retorna error HTTP 5xx, THEN THE Dashboard SHALL mostrar un mensaje de error con botón "Reintentar"
3. IF una llamada a la License_Service_API excede 10 segundos sin respuesta, THEN THE Dashboard SHALL mostrar un aviso de timeout con opción de reintentar
4. THE Dashboard SHALL cargar las secciones de forma independiente, de modo que el fallo de una sección no impida la visualización de las demás
