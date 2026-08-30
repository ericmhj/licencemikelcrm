-- V20260831: Crea los planes comerciales Esencial y Empresarial.
-- Se crean vía migración para que existan automáticamente al construir/levantar
-- el volumen (no dependen de alta manual). Idempotente: no duplica si ya existen.
--
-- Columnas: codigo, nombre, descripcion, creditos_mensuales, max_free_downloads,
--           max_usuarios, roles_autorizados, precio_mensual, costo_reporte,
--           costo_punto_muestreo.
--
-- Los valores de precio/créditos/costos son base y pueden ajustarse desde
-- /admin/planes por el platform_admin.

INSERT INTO planes (
    codigo, nombre, descripcion,
    creditos_mensuales, max_free_downloads, max_usuarios,
    roles_autorizados, precio_mensual,
    costo_reporte, costo_punto_muestreo
) VALUES
(
    'PLAN_ESENCIAL', 'Esencial',
    'Plan Esencial para equipos que inician operaciones.',
    5000, 5, 10,
    '["tecnico","asistente","manager"]', 3000.00,
    520, 35
),
(
    'PLAN_EMPRESARIAL', 'Empresarial',
    'Plan Empresarial para alto volumen de estudios y operación intensiva.',
    15000, 20, 50,
    '["tecnico","asistente","manager","admin","superusuario"]', 9000.00,
    450, 30
)
ON CONFLICT (codigo) DO NOTHING;
