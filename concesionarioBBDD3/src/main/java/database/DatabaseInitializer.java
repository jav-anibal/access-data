package database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase responsable de inicializar las tablas de la base de datos.
 * RESPONSABILIDAD ÚNICA:
 * - Leer el archivo DDL apropiado (schema-mysql.sql o schema-sqlite.sql)
 * - Ejecutar las sentencias CREATE TABLE sobre la conexión activa
 * IMPORTANTE:
 * Esta clase NO gestiona conexiones, solo ejecuta DDL.
 * La conexión debe ser proporcionada por DatabaseManager.
 * ARCHIVOS DDL:
 * - resources/schema-mysql.sql  → Para MySQL
 * - resources/schema-sqlite.sql → Para SQLite
 */
public class DatabaseInitializer {

    /**
     * Crea las tablas leyendo el archivo DDL apropiado.
     * FLUJO DE EJECUCIÓN:
     * 1. Detecta qué tipo de motor es (MySQL o SQLite)
     * 2. Determina qué archivo DDL leer
     * 3. Lee el contenido del archivo
     * 4. Divide el contenido en sentencias individuales - separadas por ;)
     * 5. Ejecuta cada sentencia CREATE TABLE
     */
    public static void crearTablas(Connection con) throws SQLException, IOException {
        // PASO 1: Detectar el tipo de motor
        TipoMotor tipo = detectarTipoMotor(con);
        System.out.println("→ Creando tablas para motor: " + tipo);

        // PASO 2: Determinar qué archivo DDL usar
        String nombreArchivoDDL = (tipo == TipoMotor.MYSQL)
                ? "schema-mysql.sql"
                : "schema-sqlite.sql";

        // PASO 3: Leer el contenido del archivo DDL
        String contenidoSQL = leerArchivoDDL(nombreArchivoDDL);

        // PASO 4: Ejecutar las sentencias SQL
        ejecutarScript(con, contenidoSQL);

        System.out.println("Todas las tablas inicializadas correctamente");
    }


    // ============================================
    // MÉTODOS PRIVADOS (lógica interna)
    // ============================================

    /**
     * Detecta el tipo de motor de base de datos.
     * Usa los metadatos de la conexión para obtener el nombre del producto.
     */
    private static TipoMotor detectarTipoMotor(Connection con) throws SQLException {
        String nombreProducto = con.getMetaData().getDatabaseProductName().toLowerCase();

        if (nombreProducto.contains("mysql")) {
            return TipoMotor.MYSQL;
        } else if (nombreProducto.contains("sqlite")) {
            return TipoMotor.SQLITE;
        } else {
            throw new SQLException("Motor de base de datos no soportado: " + nombreProducto);
        }
    }


    /**
     * Lee un archivo DDL desde la carpeta resources.
     * ¿CÓMO FUNCIONA?
     * - Usa getResourceAsStream() para leer archivos desde resources/
     * - BufferedReader lee línea por línea
     * - StringBuilder concatena todas las líneas
     * IMPORTANTE:
     * Los archivos en resources/ se empaquetan dentro del .jar
     * Por eso usamos getResourceAsStream() en lugar de FileReader
     */
    private static String leerArchivoDDL(String nombreArchivo) throws IOException {
        // Obtener el InputStream del archivo en resources/
        InputStream inputStream = DatabaseInitializer.class
                .getClassLoader()
                .getResourceAsStream(nombreArchivo);

        // Verificar que el archivo existe
        if (inputStream == null) {
            throw new IOException("No se encontró el archivo DDL: " + nombreArchivo);
        }

        // Leer el contenido línea por línea
        StringBuilder contenido = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
        }

        return contenido.toString();
    }


    /**
     * Ejecuta un script SQL que contiene múltiples sentencias.
     *
     * ¿CÓMO FUNCIONA?
     * 1. Divide el script por punto y coma (;) → cada sentencia
     * 2. Limpia cada sentencia (espacios, líneas vacías, comentarios)
     * 3. Ejecuta solo las sentencias no vacías
     *
     * MANEJO DE COMENTARIOS:
     * - Ignora líneas que empiezan con "--" (comentarios SQL)
     * - Ignora líneas vacías
     *
     * @param con conexión activa
     * @param scriptSQL contenido del archivo DDL
     * @throws SQLException si hay error al ejecutar alguna sentencia
     */
    private static void ejecutarScript(Connection con, String scriptSQL) throws SQLException {
        try (Statement stmt = con.createStatement()) {

            // Dividir el script en sentencias individuales (separadas por ;)
            String[] sentencias = scriptSQL.split(";");

            int tablasCreadas = 0;

            // Ejecutar cada sentencia
            for (String sentencia : sentencias) {
                // Limpiar la sentencia (quitar espacios y saltos de línea)
                String sentenciaLimpia = limpiarSentencia(sentencia);

                // Ejecutar solo si no está vacía
                if (!sentenciaLimpia.isEmpty()) {
                    stmt.executeUpdate(sentenciaLimpia);
                    tablasCreadas++;
                    System.out.println("Sentencia ejecutada correctamente");
                }
            }

            System.out.println("Total de sentencias ejecutadas: " + tablasCreadas);

        } catch (SQLException e) {
            System.err.println("Error al ejecutar script DDL: " + e.getMessage());

        }
    }


    /**
     * Limpia una sentencia SQL eliminando:
     * - Comentarios (líneas que empiezan con --)
     * - Espacios en blanco innecesarios
     * - Líneas vacías
     *
     * ¿POR QUÉ ES NECESARIO?
     * Los archivos .sql pueden tener comentarios y formato que debemos
     * eliminar antes de ejecutar la sentencia.
     *
     * @param sentencia sentencia SQL sin procesar
     * @return sentencia limpia y lista para ejecutar
     */
    private static String limpiarSentencia(String sentencia) {
        StringBuilder resultado = new StringBuilder();

        // Procesar línea por línea
        String[] lineas = sentencia.split("\n");
        for (String linea : lineas) {
            // Quitar espacios al inicio y final
            linea = linea.trim();

            // Ignorar líneas vacías y comentarios
            if (!linea.isEmpty() && !linea.startsWith("--")) {
                resultado.append(linea).append(" ");
            }
        }

        return resultado.toString().trim();
    }
}


