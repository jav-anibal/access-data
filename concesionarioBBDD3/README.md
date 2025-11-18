# Sistema de Gestión de Concesionario

Sistema completo de gestión para concesionarios de automóviles desarrollado en Java con soporte para bases de datos MySQL y SQLite.

## Descripción del Proyecto

Aplicación de consola interactiva que permite gestionar un concesionario de vehículos, incluyendo:
- Inventario de coches
- Registro de propietarios/clientes
- Traspasos de vehículos (ventas)
- Generación de informes estadísticos
- Importación masiva desde CSV

## Arquitectura del Sistema

### Patrón de Diseño

El proyecto implementa una **arquitectura en capas** con el **patrón DAO (Data Access Object)**:

```
Main (Punto de entrada)
    ↓
MenuPrincipal (Capa de Presentación)
    ↓
DatabaseManager (Gestión de Conexiones)
    ↓
DAO Classes (Acceso a Datos)
    ↓
MySQL / SQLite (Base de Datos)
```

### Estructura de Directorios

```
concesionarioBBDD3/
├── src/main/
│   ├── java/
│   │   ├── Main.java                          # Punto de entrada
│   │   ├── database/
│   │   │   ├── DatabaseManager.java           # Gestión de conexiones (Singleton)
│   │   │   ├── DatabaseInitializer.java       # Inicialización de esquemas
│   │   │   └── TipoMotor.java                 # Enum (MYSQL, SQLITE)
│   │   ├── model/
│   │   │   ├── PropietarioDAO.java            # Gestión de propietarios
│   │   │   ├── CocheDAO.java                  # Gestión de coches
│   │   │   ├── TraspasoDAO.java               # Gestión de traspasos/ventas
│   │   │   ├── ProcedimientoDAO.java          # Ejecución de procedimientos
│   │   │   └── InformeDAO.java                # Generación de informes
│   │   └── util/
│   │       ├── MenuPrincipal.java             # Interfaz de usuario
│   │       ├── ConfigProperties.java          # Gestor de configuración
│   │       └── ConfigException.java           # Excepción personalizada
│   └── resources/
│       ├── config.properties                   # Configuración del sistema
│       ├── schema-mysql.sql                    # Esquema para MySQL
│       ├── schema-sqlite.sql                   # Esquema para SQLite
│       └── schema-procedures.sql               # Procedimientos almacenados
├── concesionario.db                            # Base de datos SQLite
├── pom.xml                                     # Configuración Maven
└── README.md                                   # Este archivo
```

## Modelo de Datos

### Esquema de Base de Datos

El sistema utiliza tres tablas principales:

#### 1. **propietarios** (Propietarios/Clientes)

| Campo       | Tipo         | Descripción                    |
|-------------|--------------|--------------------------------|
| id_propietario | INT (PK)  | Identificador único (AUTO_INCREMENT) |
| dni         | VARCHAR      | DNI del propietario (UNIQUE)   |
| nombre      | VARCHAR      | Nombre del propietario         |
| apellidos   | VARCHAR      | Apellidos del propietario      |
| telefono    | VARCHAR      | Teléfono (opcional)            |

#### 2. **coches** (Vehículos)

| Campo          | Tipo         | Descripción                    |
|----------------|--------------|--------------------------------|
| matricula      | VARCHAR (PK) | Matrícula del vehículo         |
| marca          | VARCHAR      | Marca del coche                |
| modelo         | VARCHAR      | Modelo del coche               |
| extras         | VARCHAR      | Extras (separados por `|`)     |
| precio         | DECIMAL      | Precio del vehículo            |
| id_propietario | INT (FK)     | Propietario actual (NULL = concesionario) |

#### 3. **traspasos** (Ventas/Transferencias)

| Campo            | Tipo         | Descripción                    |
|------------------|--------------|--------------------------------|
| id_traspaso      | INT (PK)     | Identificador único (AUTO_INCREMENT) |
| matricula_coche  | VARCHAR (FK) | Matrícula del coche vendido    |
| id_vendedor      | INT (FK)     | ID del vendedor (NULL = concesionario) |
| id_comprador     | INT (FK)     | ID del comprador               |
| monto_economico  | DECIMAL      | Monto de la transacción        |

### Relaciones entre Entidades

```
propietarios (1) ─────┬──── (N) coches.id_propietario
                      │
                      ├──── (N) traspasos.id_vendedor
                      │
                      └──── (N) traspasos.id_comprador

coches (1) ────────────────── (N) traspasos.matricula_coche
```

### Reglas de Negocio

1. **Inventario del Concesionario**: Los coches con `id_propietario = NULL` pertenecen al inventario del concesionario
2. **Coches Vendidos**: Los coches con `id_propietario != NULL` están vendidos a clientes
3. **Traspasos desde Concesionario**: Cuando `id_vendedor = NULL`, el concesionario es el vendedor
4. **Traspasos entre Particulares**: Cuando `id_vendedor != NULL`, es una venta entre propietarios

## Funcionalidades del Sistema

### Menú Principal (13 Operaciones)

1. **Conectar a base de datos**: Selección entre MySQL o SQLite
2. **Inicializar base de datos**: Creación de tablas y esquema
3. **Registrar propietario**: Alta de nuevos clientes
4. **Insertar coche**: Añadir vehículos al inventario
5. **Importar coches desde CSV**: Carga masiva de vehículos
6. **Listar coches del concesionario**: Ver inventario disponible
7. **Listar coches con propietarios**: Ver vehículos vendidos
8. **Modificar datos de un coche**: Actualizar información
9. **Borrar coche**: Eliminar vehículos del sistema
10. **Realizar traspaso**: Gestionar ventas (transaccional)
11. **Crear procedimiento almacenado**: Solo MySQL
12. **Ejecutar procedimiento almacenado**: Estadísticas por marca
13. **Generar informe resumen**: Exportar análisis completo

### Características Técnicas

#### Gestión de Transacciones (ACID)

El sistema implementa control transaccional en operaciones críticas:

```java
// Ejemplo: Traspaso de vehículo (TraspasoDAO.java)
connection.setAutoCommit(false);
try {
    // 1. Validar comprador
    // 2. Validar existencia del coche
    // 3. Obtener propietario actual
    // 4. Insertar registro de traspaso
    // 5. Actualizar propietario del coche
    connection.commit(); // Todo OK
} catch (SQLException e) {
    connection.rollback(); // Revertir cambios
}
```

#### Prevención de SQL Injection

Todas las consultas utilizan `PreparedStatement`:

```java
String sql = "INSERT INTO coches (matricula, marca, modelo, extras, precio) VALUES (?, ?, ?, ?, ?)";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, matricula);
stmt.setString(2, marca);
// ...
```

#### Importación desde CSV

Formato del archivo CSV (`coches.csv`):

```csv
1234ABC;Toyota;Corolla;GPS|ABS|Climatizador;18000.00
5678DEF;Honda;Civic;GPS|Bluetooth;16500.50
```

- Separador: `;` (punto y coma)
- Campos: `matricula;marca;modelo;extras;precio`
- Extras separados por `|` (pipe)

#### Procedimiento Almacenado (MySQL)

**sp_coches_por_marca**: Genera estadísticas agrupadas por marca

```sql
SELECT
    marca,
    COUNT(*) AS total_coches,
    AVG(precio) AS precio_promedio,
    MIN(precio) AS precio_minimo,
    MAX(precio) AS precio_maximo
FROM coches
GROUP BY marca
ORDER BY total_coches DESC;
```

#### Generación de Informes

El informe resumen (`informe_concesionario.txt`) incluye:

1. Total de coches en el sistema
2. Distribución de coches por marca
3. Extra más popular entre todos los vehículos
4. Estadísticas de precios (promedio, mínimo, máximo)
5. Coches vendidos vs. inventario del concesionario

## Configuración

### Archivo `config.properties`

```properties
# Configuración MySQL
mysql.url=jdbc:mysql://localhost:3306/concesionario?serverTimezone=UTC
mysql.user=root
mysql.pass=admin

# Configuración SQLite
sqlite.path=concesionario.db

# Scripts DDL
ddl.mysql=schema-mysql.sql
ddl.sqlite=schema-sqlite.sql

# Archivos de datos
csv.path=coches.csv
informe.path=informe_concesionario.txt
```

### Requisitos Previos

- **Java**: JDK 21 o superior
- **Maven**: 3.6+ (para gestión de dependencias)
- **MySQL**: 8.0+ (opcional, si se usa MySQL)
- **SQLite**: Incluido vía JDBC driver

### Dependencias (pom.xml)

```xml
<dependencies>
    <!-- Driver MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- Driver SQLite -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.46.0.0</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.5.6</version>
    </dependency>
</dependencies>
```

## Instalación y Ejecución

### 1. Clonar el Repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd concesionarioBBDD3
```

### 2. Compilar el Proyecto

```bash
mvn clean compile
```

### 3. Ejecutar la Aplicación

```bash
mvn exec:java -Dexec.mainClass="Main"
```

O si prefieres compilar y ejecutar manualmente:

```bash
mvn package
java -jar target/concesionarioBBDD3-1.0-SNAPSHOT.jar
```

### 4. Configurar la Base de Datos

#### Opción A: MySQL

1. Instalar y arrancar MySQL Server
2. Ajustar credenciales en `config.properties`
3. En el menú: Opción 1 → MySQL
4. En el menú: Opción 2 (Inicializar base de datos)

#### Opción B: SQLite (Recomendado para pruebas)

1. No requiere instalación
2. En el menú: Opción 1 → SQLite
3. En el menú: Opción 2 (Inicializar base de datos)

## Flujo de Trabajo Típico

### Escenario 1: Venta de Coche del Concesionario a Cliente

```
1. Conectar a base de datos (MySQL o SQLite)
2. Inicializar base de datos (crear tablas)
3. Registrar propietario (cliente)
   - DNI: 12345678A
   - Nombre: Juan
   - Apellidos: García López
4. Insertar coche (inventario)
   - Matrícula: 1234ABC
   - Marca: Toyota
   - Modelo: Corolla
   - Extras: GPS|ABS
   - Precio: 18000
5. Realizar traspaso
   - DNI Comprador: 12345678A
   - Matrícula: 1234ABC
   - Monto: 18000
6. Listar coches con propietarios (verificar venta)
```

### Escenario 2: Importación Masiva y Generación de Informes

```
1. Conectar a base de datos
2. Inicializar base de datos
3. Importar coches desde CSV (carga masiva)
4. Listar coches del concesionario (ver inventario)
5. Ejecutar procedimiento almacenado (estadísticas por marca - MySQL)
6. Generar informe resumen (exportar análisis completo)
```

## Patrones de Diseño Implementados

### 1. Singleton (DatabaseManager)

Gestión centralizada de conexiones con instancia única:

```java
public class DatabaseManager {
    private static Connection connection = null;
    private static TipoMotor tipoMotor = null;

    public static Connection getConnection() {
        return connection;
    }
}
```

### 2. DAO (Data Access Object)

Separación clara entre lógica de negocio y acceso a datos:

- `PropietarioDAO` → Tabla `propietarios`
- `CocheDAO` → Tabla `coches`
- `TraspasoDAO` → Tabla `traspasos`

### 3. Layered Architecture

Separación en capas con responsabilidades específicas:

- **Presentación**: `MenuPrincipal`
- **Lógica de Negocio**: Clases DAO
- **Acceso a Datos**: `DatabaseManager`
- **Utilidades**: `ConfigProperties`, `ConfigException`

### 4. Template Method (DatabaseInitializer)

Flujo común de inicialización con implementaciones específicas:

```java
// Detecta tipo de BD → Lee script apropiado → Ejecuta DDL
crearTablas(connection);
```

### 5. Transaction Script (TraspasoDAO)

Operaciones multi-paso con propiedades ACID:

```java
BEGIN TRANSACTION
    INSERT INTO traspasos ...
    UPDATE coches SET id_propietario = ...
COMMIT / ROLLBACK
```

## Gestión de Errores

### Estrategia de Manejo de Excepciones

1. **SQLException**: Captura y mensaje amigable al usuario
2. **ConfigException**: Error específico de configuración
3. **Rollback Automático**: En caso de fallo transaccional
4. **Validaciones Previas**: Existencia de entidades antes de operar

### Ejemplo de Validación

```java
// TraspasoDAO.realizarTraspaso()
if (!existeCoche(connection, matriculaCoche)) {
    System.out.println("ERROR: El coche no existe.");
    return false;
}
```

## Características de Seguridad

1. **Prepared Statements**: Prevención de SQL Injection
2. **Validación de Entrada**: Checks de existencia y formato
3. **Transacciones ACID**: Integridad de datos garantizada
4. **Gestión de Recursos**: Try-with-resources para auto-cierre

## Soporte Multi-Base de Datos

### Diferencias entre MySQL y SQLite

| Característica            | MySQL | SQLite |
|---------------------------|-------|--------|
| Procedimientos Almacenados | ✅    | ❌     |
| AUTO_INCREMENT            | ✅    | AUTOINCREMENT |
| Tipo DECIMAL              | ✅    | REAL   |
| Cliente-Servidor          | ✅    | ❌ (Embebida) |

### Detección Automática

El sistema detecta automáticamente el tipo de base de datos:

```java
// DatabaseInitializer.detectarTipoMotor()
String productName = metaData.getDatabaseProductName();
if (productName.contains("MySQL")) return TipoMotor.MYSQL;
if (productName.contains("SQLite")) return TipoMotor.SQLITE;
```

## Contribuir

### Estructura de Commits

```bash
git commit -m "tipo: descripción breve"
```

Tipos: `feat`, `fix`, `docs`, `refactor`, `test`

### Extensiones Futuras

Posibles mejoras al proyecto:

- [ ] Interfaz gráfica (JavaFX o Swing)
- [ ] API REST con Spring Boot
- [ ] Autenticación de usuarios
- [ ] Historial de precios de vehículos
- [ ] Búsqueda avanzada con filtros
- [ ] Exportación a PDF de informes
- [ ] Sistema de reservas
- [ ] Gestión de citas y test drives

## Licencia

Este proyecto es un ejercicio educativo de gestión de bases de datos.

## Contacto

Para preguntas o sugerencias sobre el proyecto, consulta la documentación en el código fuente o abre un issue en el repositorio.

---

**Última actualización**: 2025-11-18
**Versión**: 1.0
**Java**: 21
**Bases de Datos**: MySQL 8.0+ / SQLite 3.x
