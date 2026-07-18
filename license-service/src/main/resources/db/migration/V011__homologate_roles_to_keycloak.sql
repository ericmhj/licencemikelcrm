-- Homologar roles en tabla rol_usuario con los definidos en Keycloak (mikel-crm realm)
-- Fuente de verdad: platform_admin, superusuario, admin, manager, tecnico, asistente

-- 1. Migrar datos existentes al nuevo naming
UPDATE rol_usuario SET rol = 'admin' WHERE rol = 'ADMIN_CUENTA';
UPDATE rol_usuario SET rol = 'manager' WHERE rol = 'SUPERVISOR';
UPDATE rol_usuario SET rol = 'tecnico' WHERE rol = 'TECNICO';
UPDATE rol_usuario SET rol = 'asistente' WHERE rol = 'ASISTENTE';

-- 2. Eliminar el CHECK constraint antiguo
ALTER TABLE rol_usuario DROP CONSTRAINT IF EXISTS rol_usuario_rol_check;

-- 3. Agregar nuevo CHECK constraint con roles homologados
ALTER TABLE rol_usuario ADD CONSTRAINT rol_usuario_rol_check
    CHECK (rol IN ('platform_admin', 'superusuario', 'admin', 'manager', 'tecnico', 'asistente'));
