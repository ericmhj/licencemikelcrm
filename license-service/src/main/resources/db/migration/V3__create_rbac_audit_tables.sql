-- Rol de Usuario
CREATE TABLE rol_usuario (
    usuario_id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN_CUENTA', 'SUPERVISOR', 'TECNICO', 'ASISTENTE')),
    asignado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    asignado_por UUID,
    PRIMARY KEY (usuario_id, tenant_id)
);

-- Audit Log (partitioned by month)
CREATE TABLE audit_log (
    id UUID NOT NULL,
    tenant_id UUID,
    usuario_id UUID,
    accion VARCHAR(100) NOT NULL,
    entidad VARCHAR(100) NOT NULL,
    entidad_id UUID,
    payload_antes JSONB,
    payload_despues JSONB,
    resultado VARCHAR(15) NOT NULL CHECK (resultado IN ('OK', 'DENEGADO', 'ERROR')),
    ip_origen VARCHAR(45),
    ocurrido_en TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);

-- Create partitions for current and next month
CREATE TABLE audit_log_2026_06 PARTITION OF audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE audit_log_2026_07 PARTITION OF audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

-- Prevent UPDATE and DELETE on audit_log
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: UPDATE and DELETE operations are not allowed';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();
