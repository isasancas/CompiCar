ALTER TABLE viaje 
    ADD COLUMN IF NOT EXISTS kilometros_recorridos INTEGER;

ALTER TABLE viaje_recurrente 
    ADD COLUMN IF NOT EXISTS kilometros_recorridos INTEGER;
