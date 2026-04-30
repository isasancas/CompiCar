CREATE TABLE notificacion (
    id BIGSERIAL PRIMARY KEY,
    mensaje TEXT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    receptor_id BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    CONSTRAINT fk_notificacion_receptor FOREIGN KEY (receptor_id) REFERENCES persona(id) ON DELETE CASCADE
);

CREATE INDEX idx_notificacion_receptor_leida ON notificacion(receptor_id, leida);