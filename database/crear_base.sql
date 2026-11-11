-- Ejecuta este archivo una sola vez en MySQL antes del primer arranque.
-- Las tablas las crea la propia aplicacion con Flyway (src/main/resources/db/migration).

CREATE DATABASE IF NOT EXISTS taller_manager
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Recomendado en produccion: no usar el usuario root. Quita los dos guiones
-- de las lineas siguientes, cambia la contrasena y usa estos datos en
-- DB_USER y DB_PASSWORD.
--
-- CREATE USER 'taller'@'localhost' IDENTIFIED BY 'una-clave-larga-y-propia';
-- GRANT ALL PRIVILEGES ON taller_manager.* TO 'taller'@'localhost';
-- FLUSH PRIVILEGES;
