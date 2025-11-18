package model;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Clase DAO para gestionar procedimientos almacenados
 */
public class ProcedimientoDAO {

    /**
     * Ejecuta el procedimiento almacenado sp_coches_por_marca
     * Muestra estadísticas de coches agrupados por marca
     * @param con Conexión activa (debe ser MySQL)
     */
    public static void ejecutarCochesPorMarca(Connection con) {

        // Verificar que es MySQL
        try {
            String nombreProducto = con.getMetaData().getDatabaseProductName().toLowerCase();
            if (!nombreProducto.contains("mysql")) {
                System.err.println("Este procedimiento solo funciona con MySQL");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar tipo de base de datos: " + e.getMessage());
            return;
        }

        // Llamar al procedimiento almacenado
        String sql = "{CALL sp_coches_por_marca()}";

        try (CallableStatement cstmt = con.prepareCall(sql);
             ResultSet rs = cstmt.executeQuery()) {

            System.out.println("\n=== ESTADÍSTICAS DE COCHES POR MARCA ===");
            System.out.println("─".repeat(90));
            System.out.printf("%-15s %15s %20s %20s %20s%n",
                    "MARCA", "TOTAL COCHES", "PRECIO PROMEDIO", "PRECIO MÍNIMO", "PRECIO MÁXIMO");
            System.out.println("─".repeat(90));

            boolean hayResultados = false;

            while (rs.next()) {
                hayResultados = true;

                String marca = rs.getString("marca");
                int totalCoches = rs.getInt("total_coches");
                double precioPromedio = rs.getDouble("precio_promedio");
                double precioMinimo = rs.getDouble("precio_minimo");
                double precioMaximo = rs.getDouble("precio_maximo");

                System.out.printf("%-15s %15d %20.2f€ %19.2f€ %19.2f€%n",
                        marca, totalCoches, precioPromedio, precioMinimo, precioMaximo);
            }

            System.out.println("─".repeat(90));

            if (!hayResultados) {
                System.out.println("No hay datos para mostrar");
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                System.err.println("Error: El procedimiento almacenado no existe.");
                System.err.println("Primero debe crear las tablas y el procedimiento (Opciones 2)");
            } else {
                System.err.println("Error al ejecutar procedimiento: " + e.getMessage());
            }
        }
    }
}
