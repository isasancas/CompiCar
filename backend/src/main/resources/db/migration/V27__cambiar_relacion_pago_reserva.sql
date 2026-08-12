-- 1. Añadir la nueva columna pago_id a la tabla reserva
ALTER TABLE reserva 
ADD COLUMN pago_id BIGINT;

-- 2. Migrar los datos existentes (copia el id del pago a la reserva correspondiente)
UPDATE reserva r
SET pago_id = p.id
FROM pago p
WHERE p.reserva_id = r.id; 
-- Nota: En MySQL usa la sintaxis:
-- UPDATE reserva r JOIN pago p ON p.reserva_id = r.id SET r.pago_id = p.id;

-- 3. Crear la restricción de clave foránea en la tabla reserva
ALTER TABLE reserva 
ADD CONSTRAINT fk_reserva_pago 
FOREIGN KEY (pago_id) REFERENCES pago(id);

-- 4. Eliminar la clave foránea y la columna antigua de la tabla pago
-- (Asegúrate del nombre exacto del FK en tu BD antes de ejecutar este DROP)
ALTER TABLE pago DROP CONSTRAINT IF EXISTS fk_pago_reserva; 
ALTER TABLE pago DROP COLUMN IF EXISTS reserva_id;