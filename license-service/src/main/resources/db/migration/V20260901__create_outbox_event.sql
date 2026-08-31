-- Transactional Outbox
-- Garantiza que cada evento de dominio se persista en la MISMA transacción que
-- el cambio de estado del tenant. Un publisher asíncrono los envía a Kafka con
-- reintentos, evitando la pérdida de eventos cuando Kafka no está disponible
-- en el instante del commit.

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,          -- tenant_id (clave de partición Kafka)
    topic VARCHAR(100) NOT NULL,         -- topic destino
    event_type VARCHAR(100) NOT NULL,    -- p.ej. tenant.onboarded
    payload TEXT NOT NULL,               -- JSON serializado listo para enviar
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    intentos INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    enviado_en TIMESTAMP,
    ultimo_error TEXT
);

-- El publisher busca PENDING/FAILED ordenados por creación.
CREATE INDEX idx_outbox_pending ON outbox_event (status, creado_en)
    WHERE status IN ('PENDING', 'FAILED');
