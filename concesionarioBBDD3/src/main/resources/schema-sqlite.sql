-- ============================================
-- DDL para SQLite - Concesionario
-- ============================================
-- Este archivo contiene SOLO las sentencias CREATE TABLE
-- SQLite crea automáticamente el archivo .db si no existe

-- Tabla: propietarios
-- Almacena los datos de los clientes del concesionario
CREATE TABLE IF NOT EXISTS propietarios (
    id_propietario INTEGER PRIMARY KEY AUTOINCREMENT,
    dni TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL,
    apellidos TEXT NOT NULL,
    telefono TEXT
);

-- Tabla: coches
-- Almacena el inventario de vehículos
-- IMPORTANTE: id_propietario puede ser NULL (coche sin vender)
CREATE TABLE IF NOT EXISTS coches (
    matricula TEXT PRIMARY KEY,
    marca TEXT NOT NULL,
    modelo TEXT NOT NULL,
    extras TEXT,
    precio REAL NOT NULL,
    id_propietario INTEGER NULL,
    FOREIGN KEY (id_propietario)
    REFERENCES propietarios(id_propietario)
    );

-- Tabla: traspasos
-- Registra las transacciones de compra-venta
-- id_vendedor puede ser NULL (venta del concesionario)
CREATE TABLE IF NOT EXISTS traspasos (
    id_traspaso INTEGER PRIMARY KEY AUTOINCREMENT,
    matricula_coche TEXT NOT NULL,
    id_vendedor INTEGER NULL,
    id_comprador INTEGER NOT NULL,
    monto_economico REAL NOT NULL,
    FOREIGN KEY (matricula_coche)
    REFERENCES coches(matricula),
    FOREIGN KEY (id_vendedor)
    REFERENCES propietarios(id_propietario),
    FOREIGN KEY (id_comprador)
    REFERENCES propietarios(id_propietario)
    );