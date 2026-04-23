CREATE TABLE persona_preferencia_viaje (
    persona_id BIGINT NOT NULL,
    preferencia VARCHAR(255) NOT NULL,
    CONSTRAINT fk_persona_preferencia FOREIGN KEY (persona_id) REFERENCES persona(id) ON DELETE CASCADE
);