-- =============================================================================
-- Cobro mensual por plan + Pagos SPEI + CLABEs por tenant
-- =============================================================================

-- CLABEs SPEI asignadas a cada tenant (generadas por Stripe)
CREATE TABLE IF NOT EXISTS cuenta_clabe_tenant (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    clabe           VARCHAR(18) NOT NULL UNIQUE,
    tipo            VARCHAR(30) NOT NULL,  -- FIJO_MENSUAL | VARIABLE_PREPAGO
    stripe_customer_id VARCHAR(100),
    psp             VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    creada_en       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clabe_tenant ON cuenta_clabe_tenant(tenant_id);
CREATE INDEX idx_clabe_clabe ON cuenta_clabe_tenant(clabe);

-- Cobros mensuales generados automáticamente por plan
CREATE TABLE IF NOT EXISTS cobro_mensual (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    plan_id             UUID NOT NULL,
    plan_nombre         VARCHAR(100) NOT NULL,
    periodo_mes         DATE NOT NULL,
    monto_plan          DECIMAL(10,2) NOT NULL,
    creditos_otorgados  INTEGER NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_vencimiento   DATE NOT NULL,
    fecha_pago          TIMESTAMP,
    clave_rastreo       VARCHAR(50),
    referencia_pago     VARCHAR(100),
    stripe_invoice_id   VARCHAR(100),
    generado_en         TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, periodo_mes)
);

CREATE INDEX idx_cobro_tenant ON cobro_mensual(tenant_id, periodo_mes DESC);
CREATE INDEX idx_cobro_estado ON cobro_mensual(estado, fecha_vencimiento);

-- Pagos SPEI recibidos (registro de cada depósito)
CREATE TABLE IF NOT EXISTS pago_spei_recibido (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    clabe_destino           VARCHAR(18) NOT NULL,
    monto                   DECIMAL(12,2) NOT NULL,
    clave_rastreo           VARCHAR(50) NOT NULL UNIQUE,
    ordenante_nombre        VARCHAR(200),
    ordenante_clabe         VARCHAR(18),
    concepto_pago           VARCHAR(255),
    tipo_cobro              VARCHAR(30) NOT NULL,  -- FIJO_MENSUAL | VARIABLE_PREPAGO
    estado                  VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO',
    cobro_mensual_id        UUID REFERENCES cobro_mensual(id),
    creditos_otorgados      DECIMAL(10,2),
    stripe_payment_intent_id VARCHAR(100),
    recibido_en             TIMESTAMP NOT NULL DEFAULT NOW(),
    aplicado_en             TIMESTAMP,
    payload_raw             TEXT
);

CREATE INDEX idx_pago_tenant ON pago_spei_recibido(tenant_id, recibido_en DESC);
CREATE INDEX idx_pago_clave ON pago_spei_recibido(clave_rastreo);


-- Estado de cuenta por tenant (append-only, historial de abonos y cargos)
CREATE TABLE IF NOT EXISTS estado_cuenta_tenant (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    tipo                VARCHAR(10) NOT NULL,   -- ABONO | CARGO
    monto               DECIMAL(12,2) NOT NULL,
    saldo_resultante    DECIMAL(12,2) NOT NULL,
    concepto            VARCHAR(255) NOT NULL,
    referencia          VARCHAR(100),
    clave_rastreo       VARCHAR(50),
    periodo_mes         VARCHAR(10),
    cobro_mensual_id    UUID REFERENCES cobro_mensual(id),
    pago_spei_id        UUID REFERENCES pago_spei_recibido(id),
    registrado_en       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_edocuenta_tenant ON estado_cuenta_tenant(tenant_id, registrado_en DESC);

-- Prevent UPDATE/DELETE on estado de cuenta (append-only)
CREATE OR REPLACE FUNCTION prevent_edocuenta_modification()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'estado_cuenta_tenant is append-only. UPDATE and DELETE are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_edocuenta_no_update
  BEFORE UPDATE OR DELETE ON estado_cuenta_tenant
  FOR EACH ROW EXECUTE FUNCTION prevent_edocuenta_modification();
