-- ============================================
-- DDL para MySQL - Concesionario
-- ============================================
-- Este archivo contiene SOLO las sentencias CREATE TABLE
-- NO incluye CREATE DATABASE (eso lo hace DatabaseManager)

-- Tabla: propietarios
-- Almacena los datos de los clientes del concesionario
CREATE TABLE IF NOT EXISTS propietarios (
    id_propietario INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    telefono VARCHAR(15)
    ) ENGINE=InnoDB;

-- Tabla: coches
-- Almacena el inventario de vehículos
-- IMPORTANTE: id_propietario puede ser NULL (coche sin vender)
CREATE TABLE IF NOT EXISTS coches (
    matricula VARCHAR(10) PRIMARY KEY,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    extras VARCHAR(255),
    precio DECIMAL(10, 2) NOT NULL,
    id_propietario INT NULL,
    CONSTRAINT fk_coches_propietarios
    FOREIGN KEY (id_propietario)
    REFERENCES propietarios(id_propietario)
    ) ENGINE=InnoDB;

-- Tabla: traspasos
-- Registra las transacciones de compra-venta
-- id_vendedor puede ser NULL (venta del concesionario)
CREATE TABLE IF NOT EXISTS traspasos (
    id_traspaso INT AUTO_INCREMENT PRIMARY KEY,
    matricula_coche VARCHAR(10) NOT NULL,
    id_vendedor INT NULL,
    id_comprador INT NOT NULL,
    monto_economico DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_traspasos_coches
    FOREIGN KEY (matricula_coche)
    REFERENCES coches(matricula),
    CONSTRAINT fk_traspasos_vendedor
    FOREIGN KEY (id_vendedor)
    REFERENCES propietarios(id_propietario),
    CONSTRAINT fk_traspasos_comprador
    FOREIGN KEY (id_comprador)
    REFERENCES propietarios(id_propietario)
    ) ENGINE=InnoDB;