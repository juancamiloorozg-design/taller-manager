-- Esquema inicial de taller-manager.
-- Escrito con tipos y sintaxis que aceptan tanto MySQL 8 como H2 en modo MySQL.
-- Las fechas las pone la aplicacion (zona America/Bogota), no la base de datos.

CREATE TABLE usuarios (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    email             VARCHAR(120) NOT NULL,
    nombre            VARCHAR(120) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    rol               VARCHAR(20)  NOT NULL,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    bloqueado_hasta   DATETIME     NULL,
    ultimo_acceso     DATETIME     NULL,
    creado_en         DATETIME     NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE clientes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento     VARCHAR(10)  NOT NULL,
    numero_documento   VARCHAR(30)  NOT NULL,
    nombre             VARCHAR(120) NOT NULL,
    telefono           VARCHAR(30)  NULL,
    email              VARCHAR(120) NULL,
    direccion          VARCHAR(200) NULL,
    autoriza_datos     BOOLEAN      NOT NULL,
    fecha_autorizacion DATETIME     NULL,
    version_politica   VARCHAR(20)  NULL,
    registrado_por     VARCHAR(120) NULL,
    anonimizado        BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en          DATETIME     NOT NULL,
    CONSTRAINT uk_clientes_documento UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE vehiculos (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    placa      VARCHAR(10) NOT NULL,
    marca      VARCHAR(50) NOT NULL,
    modelo     VARCHAR(50) NOT NULL,
    anio       INT         NOT NULL,
    color      VARCHAR(30) NULL,
    cliente_id BIGINT      NOT NULL,
    creado_en  DATETIME    NOT NULL,
    CONSTRAINT uk_vehiculos_placa UNIQUE (placa),
    CONSTRAINT fk_vehiculos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE TABLE repuestos (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo       VARCHAR(40)   NULL,
    nombre       VARCHAR(100)  NOT NULL,
    precio       DECIMAL(12,2) NOT NULL,
    stock        INT           NOT NULL DEFAULT 0,
    stock_minimo INT           NOT NULL DEFAULT 0,
    activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_repuestos_precio CHECK (precio >= 0),
    CONSTRAINT chk_repuestos_stock CHECK (stock >= 0)
);

CREATE TABLE ordenes_servicio (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehiculo_id    BIGINT        NOT NULL,
    descripcion    VARCHAR(500)  NOT NULL,
    kilometraje    INT           NULL,
    estado         VARCHAR(20)   NOT NULL,
    mano_obra      DECIMAL(12,2) NOT NULL DEFAULT 0,
    total          DECIMAL(12,2) NOT NULL DEFAULT 0,
    creado_por     VARCHAR(120)  NULL,
    creado_en      DATETIME      NOT NULL,
    actualizado_en DATETIME      NOT NULL,
    finalizado_en  DATETIME      NULL,
    CONSTRAINT fk_ordenes_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id),
    CONSTRAINT chk_ordenes_total CHECK (total >= 0)
);

CREATE TABLE orden_repuestos (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id        BIGINT        NOT NULL,
    repuesto_id     BIGINT        NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_or_orden FOREIGN KEY (orden_id) REFERENCES ordenes_servicio (id) ON DELETE CASCADE,
    CONSTRAINT fk_or_repuesto FOREIGN KEY (repuesto_id) REFERENCES repuestos (id),
    CONSTRAINT chk_or_cantidad CHECK (cantidad > 0),
    CONSTRAINT uk_or_orden_repuesto UNIQUE (orden_id, repuesto_id)
);

CREATE TABLE auditoria (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha      DATETIME     NOT NULL,
    usuario    VARCHAR(120) NULL,
    accion     VARCHAR(40)  NOT NULL,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id BIGINT       NULL,
    detalle    VARCHAR(500) NULL
);

CREATE INDEX idx_clientes_nombre ON clientes (nombre);
CREATE INDEX idx_vehiculos_cliente ON vehiculos (cliente_id);
CREATE INDEX idx_ordenes_estado ON ordenes_servicio (estado);
CREATE INDEX idx_ordenes_vehiculo ON ordenes_servicio (vehiculo_id);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha);
