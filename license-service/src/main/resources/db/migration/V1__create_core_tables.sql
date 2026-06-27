-- Tenant
CREATE TABLE tenant (
    id UUID PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email_contacto VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ONBOARDING' CHECK (estado IN ('ONBOARDING', 'ACTIVE', 'SUSPENDED', 'CANCELLED')),
    modalidad_reporte VARCHAR(20) NOT NULL CHECK (modalidad_reporte IN ('ESTANDAR', 'PERSONALIZADO')),
    fecha_alta TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_suspension TIMESTAMP,
    fecha_cancelacion TIMESTAMP,
    reportes_almacenados INT NOT NULL DEFAULT 0,
    deuda_almacenamiento NUMERIC(10,2) NOT NULL DEFAULT 0.00
);

-- Contrato Anual
CREATE TABLE contrato_anual (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('MODULO', 'CREDITOS')),
    modulo VARCHAR(30),
    estado VARCHAR(15) NOT NULL DEFAULT 'CREATED' CHECK (estado IN ('CREATED', 'ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED')),
    cuota_mensual NUMERIC(10,2) NOT NULL,
    cuotas_pagadas INT NOT NULL DEFAULT 0,
    cuotas_totales INT NOT NULL DEFAULT 12 CHECK (cuotas_totales = 12),
    fecha_inicio DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_aniversario INT NOT NULL CHECK (fecha_aniversario BETWEEN 1 AND 31),
    renovacion_auto BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Cuota Mensual
CREATE TABLE cuota_mensual (
    id UUID PRIMARY KEY,
    contrato_id UUID NOT NULL REFERENCES contrato_anual(id),
    numero INT NOT NULL CHECK (numero BETWEEN 1 AND 12),
    fecha_limite DATE NOT NULL,
    monto_original NUMERIC(10,2) NOT NULL,
    monto_cobrado NUMERIC(10,2),
    descuento_pct NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (descuento_pct IN (0.00, 3.00, 10.00)),
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'PAGADA', 'VENCIDA', 'EN_MORA')),
    fecha_pago TIMESTAMP,
    intento_cobro INT NOT NULL DEFAULT 0 CHECK (intento_cobro BETWEEN 0 AND 3),
    UNIQUE (contrato_id, numero)
);

-- Renovacion
CREATE TABLE renovacion (
    id UUID PRIMARY KEY,
    contrato_id UUID NOT NULL REFERENCES contrato_anual(id),
    fecha_vencimiento_contrato DATE NOT NULL,
    tarifa_renovacion NUMERIC(10,2) NOT NULL DEFAULT 99.00,
    primera_cuota NUMERIC(10,2) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'RECHAZADA', 'CANCELADA')),
    notificado_30d TIMESTAMP,
    notificado_7d TIMESTAMP,
    notificado_1d TIMESTAMP,
    procesada_en TIMESTAMP
);

-- Cuota de Almacenamiento
CREATE TABLE cuota_almacenamiento (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    periodo_mes DATE NOT NULL,
    reportes_snapshot INT NOT NULL,
    monto NUMERIC(10,2) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'PAGADA')),
    generada_en TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, periodo_mes)
);
