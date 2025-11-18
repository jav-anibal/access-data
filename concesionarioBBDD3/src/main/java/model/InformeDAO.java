package model;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase DAO para generar informes
 */
public class InformeDAO {

    /**
     * Genera un informe completo del concesionario en un archivo de texto
     * @param con Conexión activa
     * @param rutaArchivo Ruta donde se guardará el informe
     * @return true si se generó correctamente, false si hubo error
     */
    public static boolean generarInformeResumen(Connection con, String rutaArchivo) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaArchivo))) {

            // Encabezado del informe
            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("           INFORME RESUMEN DEL CONCESIONARIO\n");
            writer.write("═══════════════════════════════════════════════════════════════\n");

            // Fecha y hora de generación
            LocalDateTime ahora = LocalDateTime.now();
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            writer.write("Fecha de generación: " + ahora.format(formato) + "\n");
            writer.write("═══════════════════════════════════════════════════════════════\n\n");

            // 1. NÚMERO TOTAL DE COCHES
            writer.write("1. NÚMERO TOTAL DE COCHES\n");
            writer.write("───────────────────────────────────────────────────────────────\n");
            int totalCoches = obtenerTotalCoches(con);
            writer.write("Total de coches en la base de datos: " + totalCoches + "\n\n");

            // 2. COCHES AGRUPADOS POR MARCA
            writer.write("2. COCHES AGRUPADOS POR MARCA\n");
            writer.write("───────────────────────────────────────────────────────────────\n");
            Map<String, Integer> cochesPorMarca = obtenerCochesPorMarca(con);

            if (cochesPorMarca.isEmpty()) {
                writer.write("No hay coches registrados\n\n");
            } else {
                for (Map.Entry<String, Integer> entrada : cochesPorMarca.entrySet()) {
                    writer.write(String.format("%-20s : %d coches\n", entrada.getKey(), entrada.getValue()));
                }
                writer.write("\n");
            }

            // 3. EXTRA MÁS REPETIDO
            writer.write("3. EQUIPAMIENTO MÁS POPULAR\n");
            writer.write("───────────────────────────────────────────────────────────────\n");
            String extraMasRepetido = obtenerExtraMasRepetido(con);

            if (extraMasRepetido != null && !extraMasRepetido.isEmpty()) {
                writer.write("El equipamiento más solicitado es: " + extraMasRepetido + "\n\n");
            } else {
                writer.write("No hay datos de extras disponibles\n\n");
            }

            // 4. ESTADÍSTICAS ADICIONALES
            writer.write("4. ESTADÍSTICAS ADICIONALES\n");
            writer.write("───────────────────────────────────────────────────────────────\n");

            double precioPromedio = obtenerPrecioPromedio(con);
            double precioMinimo = obtenerPrecioMinimo(con);
            double precioMaximo = obtenerPrecioMaximo(con);
            int cochesVendidos = obtenerCochesVendidos(con);
            int cochesConcesionario = obtenerCochesConcesionario(con);

            writer.write(String.format("Precio promedio: %.2f€\n", precioPromedio));
            writer.write(String.format("Precio mínimo: %.2f€\n", precioMinimo));
            writer.write(String.format("Precio máximo: %.2f€\n", precioMaximo));
            writer.write(String.format("Coches vendidos (con propietario): %d\n", cochesVendidos));
            writer.write(String.format("Coches en inventario: %d\n", cochesConcesionario));
            writer.write("\n");

            // Pie del informe
            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("                    FIN DEL INFORME\n");
            writer.write("═══════════════════════════════════════════════════════════════\n");

            System.out.println("✓ Informe generado correctamente en: " + rutaArchivo);
            return true;

        } catch (IOException e) {
            System.err.println("Error al escribir el archivo: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.err.println("Error al obtener datos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el número total de coches
     */
    private static int obtenerTotalCoches(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coches";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Obtiene un mapa con la cantidad de coches por marca
     */
    private static Map<String, Integer> obtenerCochesPorMarca(Connection con) throws SQLException {
        Map<String, Integer> mapa = new HashMap<>();
        String sql = "SELECT marca, COUNT(*) as total FROM coches GROUP BY marca ORDER BY total DESC";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String marca = rs.getString("marca");
                int total = rs.getInt("total");
                mapa.put(marca, total);
            }
        }
        return mapa;
    }

    /**
     * Obtiene el extra/equipamiento que más se repite
     */
    private static String obtenerExtraMasRepetido(Connection con) throws SQLException {
        // Estrategia: dividir los extras por | y contar cada uno
        Map<String, Integer> contadorExtras = new HashMap<>();

        String sql = "SELECT extras FROM coches WHERE extras IS NOT NULL AND extras != ''";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String extras = rs.getString("extras");

                if (extras != null && !extras.trim().isEmpty()) {
                    // Dividir por |
                    String[] arrayExtras = extras.split("\\|");

                    for (String extra : arrayExtras) {
                        extra = extra.trim();
                        if (!extra.isEmpty()) {
                            contadorExtras.put(extra, contadorExtras.getOrDefault(extra, 0) + 1);
                        }
                    }
                }
            }
        }

        // Encontrar el extra con mayor contador
        String extraMasRepetido = null;
        int maxContador = 0;

        for (Map.Entry<String, Integer> entrada : contadorExtras.entrySet()) {
            if (entrada.getValue() > maxContador) {
                maxContador = entrada.getValue();
                extraMasRepetido = entrada.getKey();
            }
        }

        return extraMasRepetido;
    }

    /**
     * Obtiene el precio promedio de todos los coches
     */
    private static double obtenerPrecioPromedio(Connection con) throws SQLException {
        String sql = "SELECT AVG(precio) FROM coches";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    /**
     * Obtiene el precio mínimo
     */
    private static double obtenerPrecioMinimo(Connection con) throws SQLException {
        String sql = "SELECT MIN(precio) FROM coches";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    /**
     * Obtiene el precio máximo
     */
    private static double obtenerPrecioMaximo(Connection con) throws SQLException {
        String sql = "SELECT MAX(precio) FROM coches";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    /**
     * Obtiene el número de coches vendidos (con propietario)
     */
    private static int obtenerCochesVendidos(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coches WHERE id_propietario IS NOT NULL";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Obtiene el número de coches en inventario del concesionario
     */
    private static int obtenerCochesConcesionario(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coches WHERE id_propietario IS NULL";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
