-- 1. Añadir columna de fin de recurrencia al viaje principal
ALTER TABLE viaje 
ADD COLUMN fecha_fin_recurrencia TIMESTAMP NULL;

-- 2. Tabla para almacenar la lista de días de la semana de cada viaje principal
CREATE TABLE viaje_dias_semana (
    viaje_id BIGINT NOT NULL,
    dia_semana VARCHAR(20) NOT NULL,
    CONSTRAINT fk_viaje_dias_semana_viaje FOREIGN KEY (viaje_id) REFERENCES viaje(id) ON DELETE CASCADE
);

-- 3. Crear tabla para las ocurrencias recurrentes (incluye slug y checkin heredados de ViajeBase)
CREATE TABLE viaje_recurrente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(180) NOT NULL UNIQUE,
    checkin VARCHAR(6) NOT NULL,
    fecha_hora_salida TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NULL,
    estado VARCHAR(50) NOT NULL,
    plazas_disponibles INT NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    persona_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    viaje_padre_id BIGINT NOT NULL,
    CONSTRAINT fk_viaje_recurrente_persona FOREIGN KEY (persona_id) REFERENCES persona(id),
    CONSTRAINT fk_viaje_recurrente_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id),
    CONSTRAINT fk_viaje_recurrente_padre FOREIGN KEY (viaje_padre_id) REFERENCES viaje(id) ON DELETE CASCADE
);

-- 4. Modificar tabla PARADA para permitir paradas de viajes recurrentes
ALTER TABLE parada 
MODIFY COLUMN viaje_id BIGINT NULL;

ALTER TABLE parada 
ADD COLUMN viaje_recurrente_id BIGINT NULL;

ALTER TABLE parada 
ADD CONSTRAINT fk_parada_viaje_recurrente FOREIGN KEY (viaje_recurrente_id) REFERENCES viaje_recurrente(id) ON DELETE CASCADE;

ALTER TABLE parada 
ADD CONSTRAINT check_parada_viaje_exclusivo 
CHECK (
    (viaje_id IS NOT NULL AND viaje_recurrente_id IS NULL) OR 
    (viaje_id IS NULL AND viaje_recurrente_id IS NOT NULL)
);

-- 5. Modificar tabla RESERVA para permitir reservas en viajes recurrentes
ALTER TABLE reserva 
MODIFY COLUMN viaje_id BIGINT NULL;

ALTER TABLE reserva 
ADD COLUMN viaje_recurrente_id BIGINT NULL;

ALTER TABLE reserva 
ADD CONSTRAINT fk_reserva_viaje_recurrente FOREIGN KEY (viaje_recurrente_id) REFERENCES viaje_recurrente(id);

ALTER TABLE reserva 
ADD CONSTRAINT check_reserva_viaje_exclusivo 
CHECK (
    (viaje_id IS NOT NULL AND viaje_recurrente_id IS NULL) OR 
    (viaje_id IS NULL AND viaje_recurrente_id IS NOT NULL)
);