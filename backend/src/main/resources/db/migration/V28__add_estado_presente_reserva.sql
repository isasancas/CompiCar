-- 1. Eliminamos la restricción check actual
ALTER TABLE reserva DROP CONSTRAINT chk_reserva_estado;

-- 2. Volvemos a crear la restricción incluyendo el nuevo estado 'PRESENTE'
ALTER TABLE reserva ADD CONSTRAINT chk_reserva_estado 
CHECK (estado IN ('PENDIENTE', 'PAGADA', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA', 'NO_PRESENTADO', 'PRESENTE'));