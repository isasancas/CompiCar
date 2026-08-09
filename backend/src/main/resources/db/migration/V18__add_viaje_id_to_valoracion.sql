-- 1. Añadir la columna si no existe
ALTER TABLE valoracion ADD COLUMN IF NOT EXISTS viaje_id BIGINT;

-- 2. Rellenar los datos si hay nulos
UPDATE valoracion v
SET viaje_id = (
    SELECT viaje.id
    FROM viaje
    WHERE viaje.persona_id = v.valorado_id
    ORDER BY viaje.id ASC
    LIMIT 1
)
WHERE v.viaje_id IS NULL;

-- 3. Asegurar que sea NOT NULL
ALTER TABLE valoracion
    ALTER COLUMN viaje_id SET NOT NULL;

-- 4. Añadir la restricción solo si no existe
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_valoracion_viaje' 
        AND table_name = 'valoracion'
    ) THEN
        ALTER TABLE valoracion
            ADD CONSTRAINT fk_valoracion_viaje
                FOREIGN KEY (viaje_id) REFERENCES viaje(id);
    END IF;
END $$;

-- 5. Crear el índice si no existe
CREATE INDEX IF NOT EXISTS idx_valoracion_viaje_id ON valoracion(viaje_id);