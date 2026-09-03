-- =============================================================================
-- Mensualidades por tenant (renta mensual del CRM)
-- =============================================================================
-- Cada tenant tiene una fila por mes desde su fecha de alta. El pago de renta
-- se aplica al mes PENDIENTE más antiguo y avanza hacia el más reciente.
-- Un pago puede cubrir varios meses (monto ÷ mensualidad del plan).

CREATE TABLE IF NOT EXISTS mensualidad_tenant (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    periodo_mes     DATE NOT NULL,                 -- primer día del mes cubierto
    monto           DECIMAL(12,2) NOT NULL,        -- mensualidad esperada (snapshot del plan al pagar)
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',  -- PENDIENTE | PAGADA
    fecha_pago      TIMESTAMP,
    pago_spei_id    UUID,
    concepto        VARCHAR(255),
    creada_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, periodo_mes)
);

CREATE INDEX IF NOT EXISTS idx_mensualidad_tenant ON mensualidad_tenant(tenant_id, periodo_mes);
CREATE INDEX IF NOT EXISTS idx_mensualidad_estado ON mensualidad_tenant(tenant_id, estado, periodo_mes);

-- Backfill: generar las mensualidades desde el mes de alta de cada tenant hasta el mes actual.
-- El monto se toma del precio mensual del plan del tenant (0 si no tiene plan aún).
-- Las mensualidades hasta servicio_pagado_hasta se marcan PAGADA (histórico ya cubierto).
INSERT INTO mensualidad_tenant (tenant_id, periodo_mes, monto, estado, concepto)
SELECT
    t.id,
    gs.periodo::date,
    COALESCE(p.precio_mensual, 0),
    CASE
        WHEN t.servicio_pagado_hasta IS NOT NULL
             AND gs.periodo::date <= t.servicio_pagado_hasta THEN 'PAGADA'
        ELSE 'PENDIENTE'
    END,
    'Pago Renta mensualidad CRM'
FROM tenant t
LEFT JOIN planes p ON p.id = t.plan_id
CROSS JOIN LATERAL generate_series(
    date_trunc('month', t.fecha_alta)::date,
    date_trunc('month', CURRENT_DATE)::date,
    interval '1 month'
) AS gs(periodo)
ON CONFLICT (tenant_id, periodo_mes) DO NOTHING;

-- Ampliar el enum de tipos de movimiento del estado de cuenta para diferenciar
-- el pago de renta (no es un abono a saldo de crédito).
-- La columna es VARCHAR, así que no requiere ALTER TYPE.
