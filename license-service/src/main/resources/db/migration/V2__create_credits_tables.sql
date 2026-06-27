-- Paquete de Créditos
CREATE TABLE paquete_creditos (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    contrato_id UUID REFERENCES contrato_anual(id),
    creditos_paquete INT NOT NULL,
    creditos_bonus INT NOT NULL DEFAULT 4 CHECK (creditos_bonus = 4),
    saldo_disponible NUMERIC(10,2) NOT NULL CHECK (saldo_disponible >= 0),
    creditos_totales_adquiridos INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVE' CHECK (estado IN ('ACTIVE', 'EXPIRED', 'CANCELLED'))
);

-- Evento de Crédito
CREATE TABLE evento_credito (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    paquete_id UUID NOT NULL REFERENCES paquete_creditos(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CONSUMO', 'BONUS', 'COMPRA', 'EXCEDENTE', 'CADUCIDAD', 'COMPENSACION')),
    cantidad NUMERIC(10,2) NOT NULL,
    saldo_resultante NUMERIC(10,2) NOT NULL CHECK (saldo_resultante >= 0),
    perfil_documento VARCHAR(40),
    costo_creditos_aplicado NUMERIC(10,2),
    documento_id UUID,
    usuario_id UUID,
    ocurrido_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Contador de Consultas
CREATE TABLE contador_consultas (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    tipo_reporte VARCHAR(100) NOT NULL,
    total_consultas INT NOT NULL DEFAULT 0,
    periodo_anio INT NOT NULL,
    ultimo_incremento TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, tipo_reporte, periodo_anio)
);
