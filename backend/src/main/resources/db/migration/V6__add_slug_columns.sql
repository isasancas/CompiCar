ALTER TABLE persona ADD COLUMN slug VARCHAR(180);
UPDATE persona SET slug = CONCAT('persona-', id) WHERE slug IS NULL;
ALTER TABLE persona ALTER COLUMN slug SET NOT NULL;
ALTER TABLE persona ADD CONSTRAINT uk_persona_slug UNIQUE (slug);
CREATE INDEX idx_persona_slug ON persona(slug);

ALTER TABLE vehiculo ADD COLUMN slug VARCHAR(180);
UPDATE vehiculo SET slug = CONCAT('vehiculo-', id) WHERE slug IS NULL;
ALTER TABLE vehiculo ALTER COLUMN slug SET NOT NULL;
ALTER TABLE vehiculo ADD CONSTRAINT uk_vehiculo_slug UNIQUE (slug);
CREATE INDEX idx_vehiculo_slug ON vehiculo(slug);

ALTER TABLE viaje ADD COLUMN slug VARCHAR(180);
UPDATE viaje SET slug = CONCAT('viaje-', id) WHERE slug IS NULL;
ALTER TABLE viaje ALTER COLUMN slug SET NOT NULL;
ALTER TABLE viaje ADD CONSTRAINT uk_viaje_slug UNIQUE (slug);
CREATE INDEX idx_viaje_slug ON viaje(slug);

ALTER TABLE reserva ADD COLUMN slug VARCHAR(180);
UPDATE reserva SET slug = CONCAT('reserva-', id) WHERE slug IS NULL;
ALTER TABLE reserva ALTER COLUMN slug SET NOT NULL;
ALTER TABLE reserva ADD CONSTRAINT uk_reserva_slug UNIQUE (slug);
CREATE INDEX idx_reserva_slug ON reserva(slug);

ALTER TABLE valoracion ADD COLUMN slug VARCHAR(180);
UPDATE valoracion SET slug = CONCAT('valoracion-', id) WHERE slug IS NULL;
ALTER TABLE valoracion ALTER COLUMN slug SET NOT NULL;
ALTER TABLE valoracion ADD CONSTRAINT uk_valoracion_slug UNIQUE (slug);
CREATE INDEX idx_valoracion_slug ON valoracion(slug);