package model;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Clase DAO para gestionar las operaciones de la tabla coches
 */
public class CocheDAO {

    /**
     * Inserta un nuevo coche en la base de datos (sin propietario, pertenece al concesionario)
     *
     * @param con       Conexión activa a la base de datos
     * @param matricula Matrícula del coche (debe ser única)
     * @param marca     Marca del vehículo
     * @param modelo    Modelo del vehículo
     * @param extras    Equipamiento extra separado por |
     * @param precio    Precio del vehículo
     * @return true si se insertó correctamente, false si hubo error
     */
    public static boolean insertarCoche(Connection con, String matricula, String marca,
                                        String modelo, String extras, double precio) {

        String sql = "INSERT INTO coches (matricula, marca, modelo, extras, precio, id_propietario) VALUES (?, ?, ?, ?, ?, NULL)";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            // Asignar valores a los parámetros
            pstmt.setString(1, matricula);
            pstmt.setString(2, marca);
            pstmt.setString(3, modelo);
            pstmt.setString(4, extras);
            pstmt.setDouble(5, precio);
            // El sexto parámetro (id_propietario) es NULL, ya está en el SQL

            // Ejecutar INSERT
            int filasAfectadas = pstmt.executeUpdate();

            // Retornar true si se insertó al menos una fila
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error SQL al insertar coche: " + e.getMessage());
            return false;
        }
    }




    /**
     * Importa coches desde un archivo CSV de forma transaccional
     * Si hay algún error, se hace rollback y no se inserta ningún coche
     * @param con Conexión activa
     * @param rutaCSV Ruta del archivo CSV
     * @return true si se importaron todos correctamente, false si hubo error
     */
    public static boolean importarDesdeCsv(Connection con, String rutaCSV) {

        String sql = "INSERT INTO coches (matricula, marca, modelo, extras, precio, id_propietario) VALUES (?, ?, ?, ?, ?, NULL)";

        try {
            // Desactivar auto-commit para manejar transacción manualmente
            con.setAutoCommit(false);

            // Leer el archivo CSV
            BufferedReader br = new BufferedReader(new FileReader(rutaCSV));

            // Saltar la primera línea (cabecera)
            String lineaCabecera = br.readLine();

            PreparedStatement pstmt = con.prepareStatement(sql);
            String linea;
            int contador = 0;

            // Leer línea por línea
            while ((linea = br.readLine()) != null) {
                // Dividir por punto y coma
                String[] datos = linea.split(";");

                // Validar que tenga 5 campos
                if (datos.length != 5) {
                    System.err.println("Línea inválida (se esperan 5 campos): " + linea);
                    br.close();
                    pstmt.close();
                    con.rollback();
                    con.setAutoCommit(true);
                    return false;
                }

                // Extraer datos
                String matricula = datos[0].trim();
                String marca = datos[1].trim();
                String modelo = datos[2].trim();
                String extras = datos[3].trim();
                double precio = Double.parseDouble(datos[4].trim());

                // Asignar al PreparedStatement
                pstmt.setString(1, matricula);
                pstmt.setString(2, marca);
                pstmt.setString(3, modelo);
                pstmt.setString(4, extras);
                pstmt.setDouble(5, precio);

                // Ejecutar INSERT
                pstmt.executeUpdate();
                contador++;
            }

            // Cerrar recursos
            br.close();
            pstmt.close();

            // Si llegamos aquí, todo fue bien → COMMIT
            con.commit();
            con.setAutoCommit(true);

            System.out.println("✓ Se importaron " + contador + " coches correctamente");
            return true;

        } catch (FileNotFoundException e) {
            System.err.println("Error: No se encontró el archivo CSV: " + rutaCSV);
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            return false;

        } catch (IOException e) {
            System.err.println("Error al leer el archivo CSV: " + e.getMessage());
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            return false;

        } catch (NumberFormatException e) {
            System.err.println("Error: El precio en el CSV no es un número válido");
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error SQL al importar CSV: " + e.getMessage());
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            return false;
        }
    }



    /**
     * Lista todos los coches que pertenecen al concesionario (sin propietario)
     * @param con Conexión activa
     */
    public static void listarCochesConcesionario(Connection con) {
        String sql = "SELECT matricula, marca, modelo, extras, precio FROM coches WHERE id_propietario IS NULL";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n=== COCHES DEL CONCESIONARIO ===");
            System.out.println("─".repeat(100));
            System.out.printf("%-12s %-15s %-15s %-35s %10s%n",
                    "MATRÍCULA", "MARCA", "MODELO", "EXTRAS", "PRECIO");
            System.out.println("─".repeat(100));

            boolean hayCoches = false;

            while (rs.next()) {
                hayCoches = true;

                String matricula = rs.getString("matricula");
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo");
                String extras = rs.getString("extras");
                double precio = rs.getDouble("precio");

                System.out.printf("%-12s %-15s %-15s %-35s %10.2f€%n",
                        matricula, marca, modelo, extras, precio);
            }

            System.out.println("─".repeat(100));

            if (!hayCoches) {
                System.out.println("No hay coches en el inventario del concesionario");
            }

        } catch (SQLException e) {
            System.err.println("Error al listar coches del concesionario: " + e.getMessage());
        }
    }




    /**
     * Lista todos los coches que tienen propietario, mostrando datos del propietario
     * @param con Conexión activa
     */
    public static void listarCochesPropietarios(Connection con) {
        String sql = "SELECT c.matricula, c.marca, c.modelo, c.precio, " +
                "p.dni, p.nombre, p.apellidos " +
                "FROM coches c " +
                "INNER JOIN propietarios p ON c.id_propietario = p.id_propietario " +
                "WHERE c.id_propietario IS NOT NULL";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n=== COCHES DE PROPIETARIOS ===");
            System.out.println("─".repeat(110));
            System.out.printf("%-12s %-15s %-15s %10s | %-12s %-20s %-20s%n",
                    "MATRÍCULA", "MARCA", "MODELO", "PRECIO",
                    "DNI", "NOMBRE", "APELLIDOS");
            System.out.println("─".repeat(110));

            boolean hayCoches = false;

            while (rs.next()) {
                hayCoches = true;

                String matricula = rs.getString("matricula");
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo");
                double precio = rs.getDouble("precio");
                String dni = rs.getString("dni");
                String nombre = rs.getString("nombre");
                String apellidos = rs.getString("apellidos");

                System.out.printf("%-12s %-15s %-15s %10.2f€ | %-12s %-20s %-20s%n",
                        matricula, marca, modelo, precio,
                        dni, nombre, apellidos);
            }

            System.out.println("─".repeat(110));

            if (!hayCoches) {
                System.out.println("No hay coches vendidos a propietarios");
            }

        } catch (SQLException e) {
            System.err.println("Error al listar coches de propietarios: " + e.getMessage());
        }
    }






    /**
     * Modifica los datos de un coche existente (excepto la matrícula)
     * @param con Conexión activa
     * @param matricula Matrícula del coche a modificar
     * @param marca Nueva marca
     * @param modelo Nuevo modelo
     * @param extras Nuevos extras
     * @param precio Nuevo precio
     * @return true si se modificó correctamente, false si no existe o hubo error
     */
    public static boolean modificarCoche(Connection con, String matricula, String marca,
                                         String modelo, String extras, double precio) {

        String sql = "UPDATE coches SET marca = ?, modelo = ?, extras = ?, precio = ? WHERE matricula = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            // Asignar valores a los parámetros
            pstmt.setString(1, marca);
            pstmt.setString(2, modelo);
            pstmt.setString(3, extras);
            pstmt.setDouble(4, precio);
            pstmt.setString(5, matricula);

            // Ejecutar UPDATE
            int filasAfectadas = pstmt.executeUpdate();

            // Si filasAfectadas = 0, significa que no existe un coche con esa matrícula
            if (filasAfectadas == 0) {
                System.err.println("No existe ningún coche con la matrícula: " + matricula);
                return false;
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error SQL al modificar coche: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca y muestra los datos actuales de un coche por matrícula
     * @param con Conexión activa
     * @param matricula Matrícula del coche a buscar
     * @return true si existe, false si no existe
     */
    public static boolean mostrarCoche(Connection con, String matricula) {
        String sql = "SELECT matricula, marca, modelo, extras, precio FROM coches WHERE matricula = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, matricula);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n--- DATOS ACTUALES ---");
                    System.out.println("Matrícula: " + rs.getString("matricula"));
                    System.out.println("Marca: " + rs.getString("marca"));
                    System.out.println("Modelo: " + rs.getString("modelo"));
                    System.out.println("Extras: " + rs.getString("extras"));
                    System.out.println("Precio: " + rs.getDouble("precio") + "€");
                    System.out.println("----------------------\n");
                    return true;
                } else {
                    System.err.println("No existe ningún coche con la matrícula: " + matricula);
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar coche: " + e.getMessage());
            return false;
        }
    }



    /**
     * Elimina un coche de la base de datos
     * @param con Conexión activa
     * @param matricula Matrícula del coche a eliminar
     * @return true si se eliminó correctamente, false si no existe o hubo error
     */
    public static boolean borrarCoche(Connection con, String matricula) {

        String sql = "DELETE FROM coches WHERE matricula = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            // Asignar matrícula al parámetro
            pstmt.setString(1, matricula);

            // Ejecutar DELETE
            int filasAfectadas = pstmt.executeUpdate();

            // Si filasAfectadas = 0, significa que no existe un coche con esa matrícula
            if (filasAfectadas == 0) {
                System.err.println("No existe ningún coche con la matrícula: " + matricula);
                return false;
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error SQL al borrar coche: " + e.getMessage());
            return false;
        }
    }

}






