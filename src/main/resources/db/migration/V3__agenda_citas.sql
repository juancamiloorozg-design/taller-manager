CREATE TABLE citas (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehiculo_id BIGINT       NOT NULL,
    fecha_hora  DATETIME     NOT NULL,
    motivo      VARCHAR(500) NOT NULL,
    cancelada   BOOLEAN      NOT NULL DEFAULT FALSE,
    orden_id    BIGINT       NULL,
    CONSTRAINT fk_citas_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id),
    CONSTRAINT fk_citas_orden FOREIGN KEY (orden_id) REFERENCES ordenes_servicio (id),
    CONSTRAINT uk_citas_orden UNIQUE (orden_id)
);

CREATE INDEX idx_citas_fecha ON citas (fecha_hora);
