-- V20260830: Control de mensualidad por pago recibido (SPEI), sin cobro programado.
--
-- Se agrega servicio_pagado_hasta al tenant: primer día del mes cubierto por un pago.
-- El modelo deja de generar cobros mensuales automáticos; en su lugar:
--   * El pago SPEI de la mensualidad marca el mes como pagado y (re)activa el tenant.
--   * Un job diario suspende, a partir del día 2 del mes, a quien no tenga el mes pagado.

ALTER TABLE tenant
    ADD COLUMN IF NOT EXISTS servicio_pagado_hasta DATE;

-- Homologar tenants ACTIVE existentes: se consideran al corriente del mes en curso
-- para no suspenderlos indebidamente al desplegar este cambio.
UPDATE tenant
   SET servicio_pagado_hasta = date_trunc('month', CURRENT_DATE)::date
 WHERE estado = 'ACTIVE' AND servicio_pagado_hasta IS NULL;
