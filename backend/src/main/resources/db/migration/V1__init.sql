CREATE TABLE persona (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    primer_apellido VARCHAR(255) NOT NULL,
    segundo_apellido VARCHAR(255),
    contrasena VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefono VARCHAR(255) UNIQUE,
    reputacion DOUBLE PRECISION
);

CREATE TABLE vehiculo (
    id BIGSERIAL PRIMARY KEY,
    matricula VARCHAR(255) NOT NULL UNIQUE,
    marca VARCHAR(255) NOT NULL,
    modelo VARCHAR(255) NOT NULL,
    plazas INTEGER NOT NULL,
    consumo DOUBLE PRECISION NOT NULL,
    anio INTEGER NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    persona_id BIGINT NOT NULL,
    CONSTRAINT fk_vehiculo_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id),
    CONSTRAINT chk_vehiculo_tipo
        CHECK (tipo IN ('COCHE', 'MOTO', 'FURGONETA'))
);

CREATE TABLE viaje (
    id BIGSERIAL PRIMARY KEY,
    fecha_hora_salida TIMESTAMP NOT NULL,
    estado VARCHAR(30) NOT NULL,
    plazas_disponibles INTEGER NOT NULL,
    precio NUMERIC(10, 2) NOT NULL,
    persona_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    CONSTRAINT fk_viaje_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id),
    CONSTRAINT fk_viaje_vehiculo
        FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id),
    CONSTRAINT chk_viaje_estado
        CHECK (estado IN ('INICIADO', 'PENDIENTE', 'FINALIZADO', 'CANCELADO'))
);

CREATE TABLE parada (
    id BIGSERIAL PRIMARY KEY,
    fecha_hora TIMESTAMP NOT NULL,
    localizacion VARCHAR(255) NOT NULL,
    viaje_id BIGINT NOT NULL,
    CONSTRAINT fk_parada_viaje
        FOREIGN KEY (viaje_id) REFERENCES viaje(id)
);

CREATE TABLE reserva (
    id BIGSERIAL PRIMARY KEY,
    estado VARCHAR(30) NOT NULL,
    fecha_hora_reserva TIMESTAMP NOT NULL,
    persona_id BIGINT NOT NULL,
    parada_subida_id BIGINT NOT NULL,
    parada_bajada_id BIGINT NOT NULL,
    viaje_id BIGINT NOT NULL,
    CONSTRAINT fk_reserva_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id),
    CONSTRAINT fk_reserva_parada_subida
        FOREIGN KEY (parada_subida_id) REFERENCES parada(id),
    CONSTRAINT fk_reserva_parada_bajada
        FOREIGN KEY (parada_bajada_id) REFERENCES parada(id),
    CONSTRAINT fk_reserva_viaje
        FOREIGN KEY (viaje_id) REFERENCES viaje(id),
    CONSTRAINT chk_reserva_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'NO_PRESENTADO'))
);

CREATE TABLE checkin (
    id BIGSERIAL PRIMARY KEY,
    fecha_hora_conductor TIMESTAMP NOT NULL,
    fecha_hora_pasajero TIMESTAMP NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    reserva_id BIGINT NOT NULL,
    parada_id BIGINT NOT NULL,
    CONSTRAINT fk_checkin_reserva
        FOREIGN KEY (reserva_id) REFERENCES reserva(id),
    CONSTRAINT fk_checkin_parada
        FOREIGN KEY (parada_id) REFERENCES parada(id),
    CONSTRAINT chk_checkin_tipo
        CHECK (tipo IN ('INICIO_TRAYECTO_CONDUCTOR', 'LLEGADA_PARADA_RECOGIDA', 'SUBIDA_PASAJERO', 'FIN_TRAYECTO')),
    CONSTRAINT chk_checkin_estado
        CHECK (estado IN ('PENDIENTE', 'COMPLETADO', 'NO_REALIZADO'))
);

CREATE TABLE pago (
    id BIGSERIAL PRIMARY KEY,
    importe_total NUMERIC(10, 2) NOT NULL,
    importe_conductor NUMERIC(10, 2) NOT NULL,
    comision NUMERIC(10, 2) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_pago TIMESTAMP NOT NULL,
    estado VARCHAR(30) NOT NULL,
    reserva_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_pago_reserva
        FOREIGN KEY (reserva_id) REFERENCES reserva(id),
    CONSTRAINT chk_pago_estado
        CHECK (estado IN ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO'))
);

CREATE TABLE valoracion (
    id BIGSERIAL PRIMARY KEY,
    puntuacion INTEGER NOT NULL,
    comentario VARCHAR(255),
    fecha TIMESTAMP NOT NULL,
    autor_id BIGINT NOT NULL,
    valorado_id BIGINT NOT NULL,
    CONSTRAINT fk_valoracion_autor
        FOREIGN KEY (autor_id) REFERENCES persona(id),
    CONSTRAINT fk_valoracion_valorado
        FOREIGN KEY (valorado_id) REFERENCES persona(id),
    CONSTRAINT chk_valoracion_puntuacion
        CHECK (puntuacion BETWEEN 1 AND 5)
);

CREATE INDEX idx_vehiculo_persona_id ON vehiculo(persona_id);
CREATE INDEX idx_viaje_persona_id ON viaje(persona_id);
CREATE INDEX idx_viaje_vehiculo_id ON viaje(vehiculo_id);
CREATE INDEX idx_parada_viaje_id ON parada(viaje_id);
CREATE INDEX idx_reserva_persona_id ON reserva(persona_id);
CREATE INDEX idx_reserva_parada_subida_id ON reserva(parada_subida_id);
CREATE INDEX idx_reserva_parada_bajada_id ON reserva(parada_bajada_id);
CREATE INDEX idx_reserva_viaje_id ON reserva(viaje_id);
CREATE INDEX idx_checkin_reserva_id ON checkin(reserva_id);
CREATE INDEX idx_checkin_parada_id ON checkin(parada_id);
CREATE INDEX idx_valoracion_autor_id ON valoracion(autor_id);
CREATE INDEX idx_valoracion_valorado_id ON valoracion(valorado_id);
