ALTER TABLE valoracion ADD COLUMN viaje_id BIGINT;

UPDATE valoracion v
SET viaje_id = (
    SELECT viaje.id
    FROM viaje
    WHERE viaje.persona_id = v.valorado_id
    ORDER BY viaje.id ASC
    LIMIT 1
)
WHERE v.viaje_id IS NULL;

ALTER TABLE valoracion
    ALTER COLUMN viaje_id SET NOT NULL;

ALTER TABLE valoracion
    ADD CONSTRAINT fk_valoracion_viaje
        FOREIGN KEY (viaje_id) REFERENCES viaje(id);

CREATE INDEX idx_valoracion_viaje_id ON valoracion(viaje_id);