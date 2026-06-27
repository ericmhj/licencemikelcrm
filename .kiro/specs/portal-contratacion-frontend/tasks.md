# Plan de Implementación: Portal de Contratación Frontend

## Resumen

Aplicación Angular 18 SPA con dos experiencias: portal público de calculadora interactiva (`/contratacion`) y dashboard autenticado para tenants (`/dashboard`). Se implementa con standalone components, signals, Angular Material, TailwindCSS y fast-check para property-based testing. El proyecto se crea en `portal-contratacion/` con soporte Docker para contenerización.

## Tareas

- [x] 1. Scaffolding del proyecto y configuración base
  - [x] 1.1 Crear proyecto Angular 18 con Angular CLI
    - Ejecutar `ng new portal-contratacion --standalone --routing --style=scss --skip-tests=false` en la raíz del workspace
    - Configurar `tsconfig.json` con strict mode y paths aliases (`@core/`, `@features/`, `@shared/`)
    - Configurar `angular.json` con budgets de producción
    - _Requerimientos: 7.5_

  - [x] 1.2 Instalar y configurar TailwindCSS 3.x
    - Instalar dependencias: `tailwindcss`, `postcss`, `autoprefixer`
    - Crear `tailwind.config.js` con breakpoints personalizados (sm: 768px, md: 1024px, lg: 1280px)
    - Configurar colores del sistema de diseño (`--color-primary`, `--color-success`, `--color-warning`, `--color-danger`, etc.)
    - Añadir directivas de Tailwind en `styles.scss`
    - _Requerimientos: 7.1, 7.2_

  - [x] 1.3 Instalar y configurar Angular Material 18
    - Instalar `@angular/material` y `@angular/cdk`
    - Configurar tema personalizado con paleta de colores Mikel (primary: #1976D2)
    - Importar `provideAnimationsAsync()` en `app.config.ts`
    - _Requerimientos: 8.1, 8.2_

  - [x] 1.4 Configurar archivos de entorno
    - Crear `src/environments/environment.ts` con `apiUrl: 'http://localhost:8080'`
    - Crear `src/environments/environment.prod.ts` con `apiUrl` parametrizable
    - Configurar `fileReplacements` en `angular.json` para builds de producción
    - _Requerimientos: 9.4_

  - [x] 1.5 Configurar Docker y docker-compose
    - Crear `Dockerfile` multi-stage: stage 1 (node:20-alpine para build), stage 2 (nginx:alpine para servir)
    - Crear `nginx.conf` con configuración SPA (fallback a index.html para rutas Angular)
    - Crear `docker-compose.yml` con servicio `portal-contratacion` en puerto 4200, conectado a la red del backend
    - Crear `.dockerignore` con node_modules, dist, .git
    - _Requerimientos: 7.5_

  - [x] 1.6 Instalar fast-check para property-based testing
    - Instalar `fast-check` como devDependency
    - Verificar integración con Jasmine/Karma existente
    - _Requerimientos: 2.1_

- [x] 2. Capa core — Modelos, servicios, interceptores y guards
  - [x] 2.1 Crear interfaces y modelos de datos
    - Crear `src/app/core/models/api-responses.model.ts` con interfaces: `AccessResponse`, `ContractResponse`, `ContractDetail`, `CreditResponse`, `PaqueteActivo`, `ReactivationSummaryResponse`, `CuotaVencidaDetail`, `CuotaAlmacenamientoDetail`
    - Crear `src/app/core/models/calculator.model.ts` con interfaces: `ModuloDisponible`, `PaqueteCreditos`, `SubtotalItem`, `CalculadoraState`, `ResumenServicios`
    - Crear `src/app/core/models/auth.model.ts` con interfaces: `JwtPayload`, `AuthState`, `UserRole`
    - Definir datos estáticos `MODULOS_DISPONIBLES` y `PAQUETES_CREDITOS` como constantes exportadas
    - _Requerimientos: 1.1, 2.2, 3.1, 3.3_

  - [x] 2.2 Implementar AuthService
    - Crear `src/app/core/services/auth.service.ts` como servicio singleton (`providedIn: 'root'`)
    - Implementar métodos: `getToken()`, `isAuthenticated()`, `getUserRole()`, `getTenantId()`, `logout()`
    - Decodificar JWT payload sin librerías externas (atob + JSON.parse)
    - Gestionar almacenamiento de token en localStorage
    - Exponer estado reactivo con signals: `authState`, `isAuthenticated` (computed)
    - _Requerimientos: 16.1, 16.5_

  - [x] 2.3 Implementar LicenseApiService
    - Crear `src/app/core/services/license-api.service.ts` como servicio singleton
    - Implementar métodos: `getAccess(tenantId)`, `getContracts(tenantId)`, `getCredits(tenantId)`, `getReactivationSummary(tenantId)`
    - Configurar `timeout(10_000)` y `retry(1)` en cada llamada
    - Usar `environment.apiUrl` como base URL
    - _Requerimientos: 9.4, 11.6, 12.5, 17.3_

  - [x] 2.4 Implementar interceptores HTTP
    - Crear `src/app/core/interceptors/auth.interceptor.ts` como `HttpInterceptorFn`
    - Inyectar token JWT en cabecera `Authorization: Bearer {token}` para requests a `/api/`
    - Crear `src/app/core/interceptors/error.interceptor.ts` como `HttpInterceptorFn`
    - Manejar errores 401 (logout + redirect a login) y 403 (redirect a acceso-denegado)
    - _Requerimientos: 16.5, 17.2_

  - [x] 2.5 Implementar guards funcionales
    - Crear `src/app/core/guards/auth.guard.ts` como `CanActivateFn`
    - Redirigir a `/login` si no hay token válido
    - Crear `src/app/core/guards/role.guard.ts` como `CanActivateFn`
    - Leer roles permitidos de `route.data['roles']` y redirigir a `/acceso-denegado` si el rol no coincide
    - _Requerimientos: 16.1, 16.2, 16.3_

  - [x] 2.6 Configurar app.config.ts y app.routes.ts
    - Configurar `provideRouter(routes, withComponentInputBinding())` en `app.config.ts`
    - Configurar `provideHttpClient(withInterceptors([authInterceptor, errorInterceptor]))`
    - Configurar `provideAnimationsAsync()`
    - Definir rutas con lazy loading: `/contratacion` → public-portal, `/dashboard` → dashboard (con guards)
    - Añadir ruta `/acceso-denegado` y redirect por defecto a `/contratacion`
    - _Requerimientos: 16.1, 16.2_

  - [ ]* 2.7 Write property tests para AuthInterceptor y Guards
    - **Property 14: Inyección de token JWT en requests API**
    - **Property 10: Control de acceso al Dashboard por rol**
    - **Validates: Requerimientos 16.2, 16.5**

- [x] 3. Checkpoint — Verificar estructura core
  - Asegurar que todos los tests pasan, preguntar al usuario si surgen dudas.

- [x] 4. Portal Público — Calculadora y componentes interactivos
  - [x] 4.1 Crear estructura de rutas del portal público
    - Crear `src/app/features/public-portal/public-portal.routes.ts` con `PUBLIC_PORTAL_ROUTES`
    - Crear `PublicLayoutComponent` como layout standalone con header y contenido
    - Configurar rutas hijas para el flujo del portal
    - _Requerimientos: 7.1_

  - [x] 4.2 Implementar CalculatorService (lógica pura)
    - Crear `src/app/features/public-portal/services/calculator.service.ts`
    - Implementar `calcularSubtotales(modules)`: retorna array de SubtotalItem con precioMensual × 12
    - Implementar `calcularTotalAnual(modules, paquete)`: suma subtotales + precioAnual del paquete
    - Implementar `calcularCuotaMensual(totalAnual)`: totalAnual / 12
    - Implementar `calcularDescuento(monto, fechaPago, fechaLimite)`: retorna {porcentaje, montoFinal}
    - _Requerimientos: 2.1, 2.2, 2.3, 2.4, 4.1_

  - [ ]* 4.3 Write property tests para CalculatorService
    - **Property 1: Cálculo integral de la calculadora**
    - **Property 8: Función de descuento por fecha de pago**
    - **Validates: Requerimientos 2.1, 2.2, 2.3, 2.4, 12.2, 12.3, 12.4**

  - [x] 4.4 Implementar CalculatorStateService (gestión de estado con signals)
    - Crear `src/app/features/public-portal/services/calculator-state.service.ts`
    - Definir signals mutables: `_selectedModules`, `_selectedPackage`
    - Exponer signals readonly: `selectedModules`, `selectedPackage`
    - Implementar computed: `subtotales`, `totalAnual`, `cuotaMensual`
    - Implementar mutaciones: `toggleModule(module)`, `selectPackage(pkg)`, `reset()`
    - Asegurar que `toggleModule` ignora módulos obligatorios
    - _Requerimientos: 1.2, 1.3, 2.1, 3.4_

  - [ ]* 4.5 Write property tests para CalculatorStateService
    - **Property 2: Invariante CRM_BASE no desmarcable**
    - **Property 3: Toggle como inversión (idempotencia doble)**
    - **Property 4: Exclusividad mutua de paquetes de créditos**
    - **Validates: Requerimientos 1.2, 1.3, 3.4**

  - [x] 4.6 Implementar ModuleCardsComponent
    - Crear `src/app/features/public-portal/components/module-cards/module-cards.component.ts`
    - Usar `input.required<ModuloDisponible[]>()` para recibir módulos
    - Usar `output<ModuloDisponible[]>()` para emitir cambios de selección
    - Renderizar tarjetas con MatCard y MatCheckbox
    - Marcar CRM_BASE como checked + disabled
    - Aplicar estilos visuales de selección (borde `border-2 border-primary` en seleccionados)
    - Implementar `aria-checked`, `role="checkbox"` para accesibilidad
    - _Requerimientos: 1.1, 1.2, 1.3, 1.4, 1.5, 8.1, 8.2_

  - [x] 4.7 Implementar CreditPackagesComponent
    - Crear `src/app/features/public-portal/components/credit-packages/credit-packages.component.ts`
    - Renderizar radio buttons (MatRadioGroup) para selección exclusiva de paquetes
    - Mostrar: nombre del paquete, créditos incluidos, bonus de 4, precio anual
    - Opción "Sin paquete" como valor por defecto
    - Emitir `PaqueteCreditos | null` al cambiar selección
    - _Requerimientos: 3.1, 3.2, 3.3, 3.4_

  - [x] 4.8 Implementar CalculatorComponent (display)
    - Crear `src/app/features/public-portal/components/calculator/calculator.component.ts`
    - Recibir inputs: `selectedModules`, `selectedPackage`
    - Mostrar lista de subtotales por módulo con pipe de moneda EUR
    - Mostrar total anual y cuota mensual estimada
    - Usar `aria-live="polite"` para anunciar cambios a lectores de pantalla
    - Crear `CurrencyEurPipe` en shared/pipes para formateo en euros
    - _Requerimientos: 2.1, 2.2, 2.3, 2.4, 2.5, 8.4_

  - [x] 4.9 Implementar DiscountPolicyComponent
    - Crear `src/app/features/public-portal/components/discount-policy/discount-policy.component.ts`
    - Mostrar los tres niveles de descuento en tarjetas comparativas: 10%, 3%, 0%
    - Visible sin interacción adicional (no desplegable)
    - _Requerimientos: 4.1, 4.2, 4.3_

  - [x] 4.10 Implementar ServiceSummaryComponent
    - Crear `src/app/features/public-portal/components/service-summary/service-summary.component.ts`
    - Generar texto de resumen con: módulos seleccionados, precios, total anual, cuota mensual, política de descuentos, fecha de generación
    - Implementar botón "Copiar al portapapeles" usando `navigator.clipboard.writeText()`
    - Mostrar confirmación visual temporal (snackbar/toast) al copiar exitosamente
    - Implementar fallback de selección de texto si clipboard API no está soportada
    - _Requerimientos: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 4.11 Write property test para generación de resumen
    - **Property 5: Completitud del resumen de servicios**
    - **Validates: Requerimientos 5.1, 5.2**

  - [x] 4.12 Implementar ContractRequestComponent
    - Crear `src/app/features/public-portal/components/contract-request/contract-request.component.ts`
    - Botón "Solicitar contrato" que valida selección mínima (CRM_Base)
    - Codificar selección (módulos + paquete) en query params o NavigationExtras state
    - Implementar función de encode/decode de selección para round-trip
    - Redirigir al flujo de alta con la selección codificada
    - _Requerimientos: 6.1, 6.2, 6.3_

  - [ ]* 4.13 Write property test para codificación de selección
    - **Property 6: Round-trip de codificación de selección**
    - **Validates: Requerimientos 6.1, 6.3**

  - [x] 4.14 Integrar componentes en PublicLayoutComponent
    - Componer todos los componentes del portal público en el layout
    - Conectar flujo de datos: ModuleCards → CalculatorState → Calculator
    - Conectar CreditPackages → CalculatorState
    - Añadir secciones de DiscountPolicy, ServiceSummary y ContractRequest
    - Aplicar layout responsive con TailwindCSS grid (1 col mobile, 2 col tablet, sidebar desktop)
    - _Requerimientos: 7.1, 7.2, 7.3_

- [x] 5. Checkpoint — Verificar portal público
  - Asegurar que todos los tests pasan, preguntar al usuario si surgen dudas.

- [x] 6. Dashboard — Panel de gestión del tenant autenticado
  - [x] 6.1 Crear estructura de rutas del dashboard
    - Crear `src/app/features/dashboard/dashboard.routes.ts` con `DASHBOARD_ROUTES`
    - Crear `DashboardLayoutComponent` como layout standalone con header autenticado
    - Incluir botón "Cerrar sesión" y nombre del tenant en header
    - _Requerimientos: 16.1_

  - [x] 6.2 Implementar DashboardStateService
    - Crear `src/app/features/dashboard/services/dashboard-state.service.ts`
    - Definir signals de loading por sección: `contractLoading`, `creditsLoading`
    - Definir signals de error por sección: `contractError`, `creditsError`
    - Definir signals de datos: `contractData`, `creditData`, `accessData`
    - Implementar `loadAllSections()` que carga datos en paralelo
    - Implementar `retrySection(section)` para reintentos individuales
    - Implementar `mapError(err)` para categorizar errores (TIMEOUT, SERVER_ERROR, UNKNOWN_ERROR)
    - _Requerimientos: 17.1, 17.2, 17.3, 17.4_

  - [x] 6.3 Implementar ContractSectionComponent
    - Crear `src/app/features/dashboard/components/contract-section/contract-section.component.ts`
    - Mostrar: código de contrato, fecha inicio, fecha vencimiento, días restantes, estado
    - Calcular días restantes como computed signal
    - Aplicar indicador visual por estado: ACTIVO (verde), SUSPENDIDO (rojo/advertencia), EN RENOVACIÓN (azul/informativo)
    - _Requerimientos: 9.1, 9.2, 9.3_

  - [x] 6.4 Implementar ModulesSectionComponent
    - Crear `src/app/features/dashboard/components/modules-section/modules-section.component.ts`
    - Mostrar módulos activos: nombre, precio mensual, fecha activación
    - Mostrar módulos no contratados en estilo atenuado (gris)
    - Condicionar botón "Añadir módulo" según rol (visible solo para ADMIN_CUENTA)
    - _Requerimientos: 10.1, 10.2, 10.3, 10.4_

  - [x] 6.5 Implementar CreditsSectionComponent con barra de progreso
    - Crear `src/app/features/dashboard/components/credits-section/credits-section.component.ts`
    - Mostrar: créditos totales, consumidos mes, consumidos acumulados, saldo disponible
    - Implementar computed signals: `consumoAcumulado`, `porcentajeConsumo`, `colorBarra`
    - Crear `ProgressBarComponent` en shared con input de porcentaje y color
    - Implementar semáforo: verde (≤80%), amarillo (80-95%), rojo (>95%)
    - _Requerimientos: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [ ]* 6.6 Write property test para función de color de barra de progreso
    - **Property 7: Función de color de barra de progreso**
    - **Validates: Requerimientos 11.3, 11.4, 11.5**

  - [x] 6.7 Implementar QuotaSectionComponent
    - Crear `src/app/features/dashboard/components/quota-section/quota-section.component.ts`
    - Mostrar: monto próxima cuota, fecha límite de pago, descuento vigente hoy
    - Calcular descuento actual basado en fecha del día vs. fecha límite
    - Mostrar mensaje contextual: "10% si paga hoy", "3% si paga hoy", "Sin descuento"
    - _Requerimientos: 12.1, 12.2, 12.3, 12.4_

  - [x] 6.8 Implementar HistoryTableComponent
    - Crear `src/app/features/dashboard/components/history-table/history-table.component.ts`
    - Mostrar tabla con últimos 6 meses de consumo: columnas mes y créditos consumidos
    - Ordenar del más reciente al más antiguo
    - Rellenar con 0 los meses sin datos
    - Usar MatTable para renderizado accesible
    - _Requerimientos: 13.1, 13.2, 13.3, 13.4_

  - [ ]* 6.9 Write property test para historial de consumo
    - **Property 13: Historial de consumo ordenado y completo**
    - **Validates: Requerimientos 13.2, 13.3**

  - [x] 6.10 Implementar ActionButtonsComponent
    - Crear `src/app/features/dashboard/components/action-buttons/action-buttons.component.ts`
    - Mostrar botones: "Comprar créditos adicionales", "Añadir módulo", "Ver historial de facturas"
    - Condicionar visibilidad según rol con computed signal `isAdmin`
    - SUPERVISOR: solo "Ver historial de facturas" visible
    - ADMIN_CUENTA: todos los botones visibles
    - _Requerimientos: 14.1, 14.2, 14.3, 14.4_

  - [ ]* 6.11 Write property test para visibilidad de acciones según rol
    - **Property 9: Visibilidad de acciones según rol**
    - **Validates: Requerimientos 10.3, 14.2, 14.3, 16.4**

  - [x] 6.12 Implementar BannerPromoComponent
    - Crear `src/app/features/dashboard/components/banner-promo/banner-promo.component.ts`
    - Implementar computed `showRenovacion`: true si vencimiento ≤ 30 días y no descartado
    - Implementar computed `showCreditosBaratos`: true si saldo ≤ 5% del total y no descartado
    - Gestionar `dismissedBanners` con signal de Set
    - Priorizar banner de renovación arriba cuando ambos se muestran
    - Botón de cerrar (×) por banner que persiste solo en sesión
    - _Requerimientos: 15.1, 15.2, 15.3, 15.4_

  - [ ]* 6.13 Write property tests para banners promocionales
    - **Property 11: Banner de renovación por ventana temporal**
    - **Property 12: Banner de créditos bajos por umbral**
    - **Validates: Requerimientos 15.1, 15.2, 15.4**

  - [x] 6.14 Integrar componentes en DashboardLayoutComponent
    - Componer todas las secciones del dashboard en el layout
    - Conectar DashboardStateService con cada componente vía inputs
    - Implementar patrón loading/error/content por sección con `@if`
    - Crear `LoadingSkeletonComponent` y `ErrorHandlerComponent` en shared
    - Aplicar layout responsive: grid de 2 columnas desktop, 1 columna mobile
    - _Requerimientos: 17.1, 17.4_

- [x] 7. Checkpoint — Verificar dashboard
  - Asegurar que todos los tests pasan, preguntar al usuario si surgen dudas.

- [ ] 8. Responsive, accesibilidad y componentes compartidos
  - [ ] 8.1 Implementar layouts responsive con TailwindCSS
    - Aplicar breakpoints: 1 columna (<768px), 2 columnas (768-1024px), 3-4 columnas (>1024px)
    - Calculadora sticky bottom en mobile o accesible con un toque
    - Dashboard grid adaptativo por sección
    - Botones y áreas táctiles con mínimo 44×44px (`min-h-[44px] min-w-[44px]`)
    - _Requerimientos: 7.1, 7.2, 7.3, 7.4_

  - [ ] 8.2 Implementar atributos ARIA y navegación por teclado
    - Añadir `aria-label` descriptivos en todos los elementos interactivos
    - Añadir `aria-live="polite"` en la región de la calculadora para anunciar cambios
    - Verificar que Tab/Enter/Space funcione correctamente en tarjetas y botones
    - Asegurar contraste mínimo 4.5:1 en todas las combinaciones de color del sistema
    - Añadir `role="progressbar"` con `aria-valuenow`, `aria-valuemin`, `aria-valuemax` en barra de progreso
    - _Requerimientos: 8.1, 8.2, 8.3, 8.4_

  - [ ] 8.3 Crear pipes compartidos
    - Crear `src/app/shared/pipes/currency-eur.pipe.ts` para formateo de moneda en euros (€)
    - Crear `src/app/shared/pipes/date-es.pipe.ts` para formateo de fechas en español
    - _Requerimientos: 2.2, 9.1_

  - [ ] 8.4 Crear componente AccessDeniedComponent
    - Crear `src/app/shared/components/access-denied/access-denied.component.ts`
    - Mostrar mensaje de acceso denegado con enlace para volver al portal público
    - _Requerimientos: 16.3_

- [ ] 9. Testing — Unit tests y property-based tests finales
  - [ ]* 9.1 Write unit tests para componentes del portal público
    - Tests para ModuleCardsComponent: selección, CRM_BASE deshabilitado, estilos
    - Tests para CalculatorComponent: renderizado de subtotales, total, cuota
    - Tests para CreditPackagesComponent: selección exclusiva, emisión de eventos
    - Tests para ServiceSummaryComponent: generación de texto, copia al portapapeles
    - _Requerimientos: 1.1, 2.1, 3.1, 5.1_

  - [ ]* 9.2 Write unit tests para componentes del dashboard
    - Tests para ContractSectionComponent: estados visuales, días restantes
    - Tests para CreditsSectionComponent: cálculo de porcentaje, color de barra
    - Tests para BannerPromoComponent: condiciones de visibilidad, dismiss
    - Tests para ActionButtonsComponent: visibilidad por rol
    - _Requerimientos: 9.1, 11.1, 14.1, 15.1_

  - [ ]* 9.3 Write integration tests para servicios HTTP
    - Tests para LicenseApiService con HttpClientTestingModule
    - Verificar URLs, métodos HTTP, headers de autorización
    - Tests para auth.interceptor: inyección de token en requests `/api/`
    - Tests para error.interceptor: redirección en 401 y 403
    - Tests para timeout y retry de llamadas API
    - _Requerimientos: 9.4, 16.5, 17.2, 17.3_

- [x] 10. Checkpoint final — Verificar toda la aplicación
  - Asegurar que todos los tests pasan, preguntar al usuario si surgen dudas.

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requerimientos específicos para trazabilidad
- Los checkpoints aseguran validación incremental
- Los property tests validan propiedades universales de corrección (14 properties con fast-check, 100 runs cada una)
- Los unit tests validan ejemplos específicos y casos borde
- El proyecto se sirve en contenedor Docker con nginx para producción, alineado con la infraestructura del backend existente
