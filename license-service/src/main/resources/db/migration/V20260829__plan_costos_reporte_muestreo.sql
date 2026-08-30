-- V20260829: Homologa todos los planes agregando costos por reporte y por punto de muestreo.
-- Estos costos se descuentan de la cartera del tenant cada vez que se genera un reporte
-- de estudio. Varían por plan (Profesional vs Empresarial tienen precios distintos).

-- 1. Agregar columnas a la tabla de planes (con valores por defecto para homologar).
ALTER TABLE planes
    ADD COLUMN IF NOT EXISTS costo_reporte INTEGER NOT NULL DEFAULT 520;

ALTER TABLE planes
    ADD COLUMN IF NOT EXISTS costo_punto_muestreo INTEGER NOT NULL DEFAULT 35;

-- 2. Homologar los planes existentes con los valores base.
--    Cada plan puede ajustarse individualmente desde la vista de administración.
UPDATE planes SET costo_reporte = 520, costo_punto_muestreo = 35
 WHERE costo_reporte IS NULL OR costo_punto_muestreo IS NULL;

-- 3. Alinear la terminología del plan Enterprise → Empresarial.
UPDATE planes
   SET nombre = 'Plan Empresarial'
 WHERE codigo = 'PLAN_ENTERPRISE';

-- Nota: los valores por defecto (520 / 35) aplican a los tres planes.
-- El administrador puede diferenciarlos por plan desde /admin/planes.
