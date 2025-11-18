package model;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Clase DAO para gestionar las operaciones de la tabla propietarios
 */
public class PropietarioDAO {

    /**
     * Registra un nuevo propietario en la base de datos
     * @param con Conexión activa a la base de datos
     * @param dni DNI del propietario (debe ser único)
     * @param nombre Nombre del propietario
     * @param apellidos Apellidos del propietario
     * @param telefono Teléfono de contacto
     * @return true si se insertó correctamente, false si hubo error
     */
    public static boolean registrarPropietario(Connection con, String dni, String nombre,
                                               String apellidos, String telefono) {

        String sql = "INSERT INTO propietarios (dni, nombre, apellidos, telefono) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            // Asignar valores a los parámetros
            pstmt.setString(1, dni);
            pstmt.setString(2, nombre);
            pstmt.setString(3, apellidos);
            pstmt.setString(4, telefono);

            // Ejecutar INSERT
            int filasAfectadas = pstmt.executeUpdate();

            // Retornar true si se insertó al menos una fila
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error SQL al registrar propietario: " + e.getMessage());
            return false;
        }
    }
}