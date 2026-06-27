-- Remove the rigid CHECK constraint that forces creditos_bonus = 4.
-- Welcome credits from Apertura have bonus = 0, while annual packages have bonus = 4.
ALTER TABLE paquete_creditos DROP CONSTRAINT IF EXISTS paquete_creditos_creditos_bonus_check;

-- Add a softer constraint: bonus must be 0 or 4
ALTER TABLE paquete_creditos ADD CONSTRAINT paquete_creditos_creditos_bonus_check
    CHECK (creditos_bonus IN (0, 4));
