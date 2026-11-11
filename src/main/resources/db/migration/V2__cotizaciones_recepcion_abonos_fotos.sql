-- Cotizaciones, recordatorios de mantenimiento, recepcion del vehiculo,
-- mecanico asignado, abonos y fotos. Sintaxis compatible con MySQL 8 y H2.

ALTER TABLE ordenes_servicio ADD COLUMN aprobada_en           DATETIME     NULL;
ALTER TABLE ordenes_servicio ADD COLUMN proxima_fecha         DATE         NULL;
ALTER TABLE ordenes_servicio ADD COLUMN proximo_km            INT          NULL;
ALTER TABLE ordenes_servicio ADD COLUMN recordatorio_atendido BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE ordenes_servicio ADD COLUMN nivel_combustible     VARCHAR(20)  NULL;
ALTER TABLE ordenes_servicio ADD COLUMN danos_visibles        VARCHAR(500) NULL;
ALTER TABLE ordenes_servicio ADD COLUMN objetos_dejados       VARCHAR(500) NULL;
ALTER TABLE ordenes_servicio ADD COLUMN mecanico_id           BIGINT       NULL;
ALTER TABLE ordenes_servicio ADD CONSTRAINT fk_ordenes_mecanico FOREIGN KEY (mecanico_id) REFERENCES usuarios (id);

CREATE TABLE pagos (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id      BIGINT        NOT NULL,
    monto         DECIMAL(12,2) NOT NULL,
    metodo        VARCHAR(20)   NOT NULL,
    nota          VARCHAR(200)  NULL,
    registrado_por VARCHAR(120) NULL,
    fecha         DATETIME      NOT NULL,
    CONSTRAINT fk_pagos_orden FOREIGN KEY (orden_id) REFERENCES ordenes_servicio (id),
    CONSTRAINT chk_pagos_monto CHECK (monto > 0)
);

CREATE TABLE orden_fotos (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id       BIGINT       NOT NULL,
    nombre_archivo VARCHAR(80)  NOT NULL,
    tipo_contenido VARCHAR(30)  NOT NULL,
    subida_por     VARCHAR(120) NULL,
    creada_en      DATETIME     NOT NULL,
    CONSTRAINT fk_fotos_orden FOREIGN KEY (orden_id) REFERENCES ordenes_servicio (id)
);

CREATE INDEX idx_pagos_orden ON pagos (orden_id);
CREATE INDEX idx_fotos_orden ON orden_fotos (orden_id);
CREATE INDEX idx_ordenes_recordatorio ON ordenes_servicio (estado, proxima_fecha);
