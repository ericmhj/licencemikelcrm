-- V010: Create planes catalog table with role authorization
CREATE TABLE IF NOT EXISTS planes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    creditos_mensuales INTEGER NOT NULL DEFAULT 100,
    max_free_downloads INTEGER NOT NULL DEFAULT 3,
    max_usuarios INTEGER NOT NULL DEFAULT 5,
    roles_autorizados TEXT NOT NULL DEFAULT '["tecnico"]',
    precio_mensual DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed initial plans
INSERT INTO planes (codigo, nombre, descripcion, creditos_mensuales, max_free_downloads, max_usuarios, roles_autorizados, precio_mensual) VALUES
('PLAN_BASICO', 'Plan Básico', 'Ideal para equipos pequeños con un solo técnico', 100, 3, 5, '["tecnico"]', 499.00),
('PLAN_PRO', 'Plan Profesional', 'Para equipos medianos con soporte y gestión de clientes', 500, 10, 20, '["tecnico","asistente","manager"]', 1499.00),
('PLAN_ENTERPRISE', 'Plan Enterprise', 'Acceso completo con usuarios y créditos ilimitados', -1, -1, -1, '["tecnico","asistente","manager","admin","superusuario"]', 4999.00)
ON CONFLICT (codigo) DO NOTHING;

-- Index for fast lookup by codigo
CREATE INDEX IF NOT EXISTS idx_planes_codigo ON planes(codigo);
CREATE INDEX IF NOT EXISTS idx_planes_activo ON planes(activo);

-- Add plan reference to tenant table
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS plan_id UUID REFERENCES planes(id);

-- Update existing tenants to link to PLAN_PRO by default
UPDATE tenant SET plan_id = (SELECT id FROM planes WHERE codigo = 'PLAN_PRO') WHERE plan_id IS NULL;
