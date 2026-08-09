ALTER TABLE viaje ADD COLUMN checkin VARCHAR(6) DEFAULT 'XXXXXX';

-- Intentamos rellenar con valores aleatorios (Postgres)
UPDATE viaje SET checkin = substr(md5(random()::text),1,6) WHERE checkin IS NULL OR checkin = 'XXXXXX';

ALTER TABLE viaje ALTER COLUMN checkin SET NOT NULL;
