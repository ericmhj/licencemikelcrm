-- Hot path: validación de acceso por tenant y estado
CREATE INDEX idx_contrato_tenant_estado
    ON contrato_anual (tenant_id, estado)
    WHERE estado IN ('ACTIVE', 'SUSPENDED');

-- Cron jobs: cuotas vencidas
CREATE INDEX idx_cuota_fecha_estado
    ON cuota_mensual (fecha_limite, estado)
    WHERE estado = 'PENDIENTE';

-- Cron jobs: renovaciones próximas
CREATE INDEX idx_contrato_vencimiento_auto
    ON contrato_anual (fecha_vencimiento, renovacion_auto)
    WHERE estado = 'ACTIVE' AND renovacion_auto = true;

-- Contadores de consultas por tenant y período
CREATE INDEX idx_contador_tenant_periodo
    ON contador_consultas (tenant_id, periodo_anio);

-- Eventos de crédito por tenant y fecha
CREATE INDEX idx_evento_credito_tenant_fecha
    ON evento_credito (tenant_id, ocurrido_en DESC);

-- Paquete de créditos activo por tenant
CREATE INDEX idx_paquete_tenant_estado
    ON paquete_creditos (tenant_id, estado)
    WHERE estado = 'ACTIVE';

-- Cuotas de almacenamiento pendientes por tenant
CREATE INDEX idx_cuota_almacenamiento_tenant
    ON cuota_almacenamiento (tenant_id, estado)
    WHERE estado = 'PENDIENTE';
