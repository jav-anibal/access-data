package database;

import util.ConfigProperties;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor centralizado de conexiones a base de datos.
 *
 * RESPONSABILIDAD ÚNICA:
 * - Conectar a MySQL o SQLite
 * - Mantener UNA conexión activa durante toda la ejecución
 * - Proporcionar esa conexión a otras clases cuando la necesiten
 *
 * PATRÓN DE DISEÑO:
 * - Singleton implícito (atributos y métodos estáticos)
 * - Solo existe UNA conexión compartida por toda la aplicación
 *
 * FLUJO TÍPICO:
 * 1. Usuario elige conectar MySQL o SQLite
 * 2. DatabaseManager.conectarMySQL() o conectarSQLite()
 * 3. La conexión queda guardada internamente
 * 4. Otras clases obtienen la conexión con getConnection()
 * 5. Al final: cerrarConexion()
 */
public class DatabaseManager {

    // ============================================
    // ATRIBUTOS PRIVADOS (estado interno)
    // ============================================

    /**
     * Conexión activa actual.
     * - Es static porque queremos UNA SOLA conexión para toda la app
     * - Es private para que nadie la modifique directamente
     * - null = no hay conexión activa
     */
    private static Connection conexionActiva = null;

    /**
     * Tipo de motor actual (MYSQL o SQLITE).
     * - Permite saber qué BD estamos usando sin consultar la conexión
     */
    private static TipoMotor tipoActual = null;


    // ============================================
    // MÉTODOS PÚBLICOS
    // ============================================

    /**
     * Conecta a MySQL y crea la base de datos si no existe.
     *
     * PASOS QUE REALIZA:
     * 1. Lee configuración desde config.properties
     * 2. Extrae la URL base (sin el nombre de la BD)
     * 3. Se conecta al servidor MySQL (conexión temporal)
     * 4. Ejecuta: CREATE DATABASE IF NOT EXISTS concesionario
     * 5. Cierra la conexión temporal
     * 6. Abre conexión definitiva a la BD "concesionario"
     * 7. Guarda la conexión en conexionActiva
     *
     * @return true si la conexión fue exitosa, false si hubo error
     */
    public static boolean conectarMySQL() {
        try {
            // PASO 1: Leer configuración
            String url = ConfigProperties.getProperty("mysql.url");
            String user = ConfigProperties.getProperty("mysql.user");
            String pass = ConfigProperties.getProperty("mysql.pass");

            // PASO 2: Extraer URL base (sin nombre de BD)
            // Ejemplo: jdbc:mysql://localhost:3306/concesionario → jdbc:mysql://localhost:3306/
            String urlBase = url.substring(0, url.lastIndexOf('/') + 1);

            // PASO 3: Conexión temporal al servidor (sin BD específica)
            try (Connection connTemp = DriverManager.getConnection(urlBase, user, pass);
                 Statement stmt = connTemp.createStatement()) {

                // PASO 4: Crear la base de datos si no existe
                String sqlCreateDB = "CREATE DATABASE IF NOT EXISTS concesionario";
                stmt.executeUpdate(sqlCreateDB);
                System.out.println("Base de datos -> concesionario -> Activada!!");
            }
            // La conexión temporal se cierra automáticamente (try-with-resources)

            // PASO 5: Conexión definitiva a la BD concesionario
            conexionActiva = DriverManager.getConnection(url, user, pass);
            tipoActual = TipoMotor.MYSQL;

            System.out.println("Conexión MySQL establecida");
            return true;

        } catch (SQLException e) {
            // Si algo falla, mostramos el error y retornamos false
            System.err.println("Error al conectar MySQL: " + e.getMessage());
            return false;
        }
    }


    /**
     * Conecta a SQLite (base de datos embebida en archivo).
     *
     * DIFERENCIAS CON MYSQL:
     * - No necesita servidor (es un archivo local)
     * - No hay que crear la BD explícitamente (SQLite la crea automáticamente)
     * - Más simple: solo necesitamos la ruta del archivo
     *
     * PASOS QUE REALIZA:
     * 1. Lee la ruta del archivo desde config.properties
     * 2. Se conecta (SQLite crea el archivo .db si no existe)
     * 3. Guarda la conexión en conexionActiva
     *
     * @return true si la conexión fue exitosa, false si hubo error
     */
    public static boolean conectarSQLite() {

        try {
            // PASO 1: Leer ruta del archivo desde config.properties
            String path = ConfigProperties.getProperty("sqlite.path");

            // Si no está en config.properties, usar valor por defecto
            if (path == null || path.isEmpty()) {
                path = "concesionario.db";
            }

            // PASO 2: Construir la URL de conexión JDBC para SQLite
            String url = "jdbc:sqlite:" + path;

            // PASO 3: Conectar (crea el archivo automáticamente si no existe)
            conexionActiva = DriverManager.getConnection(url);
            tipoActual = TipoMotor.SQLITE;

            System.out.println("Conexión SQLite establecida (" + path + ")");
            return true;

        } catch (SQLException e) {
            System.err.println("Error al conectar SQLite: " + e.getMessage());
            return false;
        }
    }


    /**
     * Obtiene la conexión activa actual.
     */
    public static Connection getConnection() throws SQLException {
        // Verificar que existe una conexión
        if (conexionActiva == null) {
            throw new SQLException("No hay conexión activa");
        }

        // Verificar que no está cerrada
        if (conexionActiva.isClosed()) {
            throw new SQLException("La conexión está cerrada");
        }

        return conexionActiva;
    }


    /**
     * Obtiene el tipo de motor actual (MYSQL o SQLITE).

     */
    public static TipoMotor getTipoMotor() throws SQLException {
        if (tipoActual == null) {
            throw new SQLException("No hay conexión activa");
        }
        return tipoActual;
    }


    /**
     * Verifica si hay una conexión activa y funcionando.
     */
    public static boolean isConectado() {
        try {
            // Debe cumplir AMBAS condiciones:
            // 1. Que conexionActiva no sea null
            // 2. Que la conexión no esté cerrada
            return conexionActiva != null && !conexionActiva.isClosed();
        } catch (SQLException e) {
            // Si hay error al verificar, consideramos que no hay conexión
            return false;
        }
    }



    public static void cerrarConexion() {
        try {
            if (conexionActiva != null && !conexionActiva.isClosed()) {
                conexionActiva.close();
                System.out.println("Conexión cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        } finally {
            // Siempre limpiamos el estado, incluso si hubo error
            conexionActiva = null;
            tipoActual = null;
        }
    }
}
