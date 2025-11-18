package model;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Clase DAO para gestionar las operaciones de la tabla traspasos
 */
public class TraspasoDAO {

    /**
     * Verifica si existe un propietario con el DNI dado
     * @param con Conexión activa
     * @param dni DNI del propietario
     * @return id_propietario si existe, -1 si no existe
     */
    public static int obtenerIdPropietario(Connection con, String dni) throws SQLException {
        String sql = "SELECT id_propietario FROM propietarios WHERE dni = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, dni);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_propietario");
                }
            }
        }
        return -1; // No existe
    }

    /**
     * Verifica si existe un coche con la matrícula dada
     * @param con Conexión activa
     * @param matricula Matrícula del coche
     * @return true si existe, false si no existe
     */
    public static boolean existeCoche(Connection con, String matricula) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coches WHERE matricula = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, matricula);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Obtiene el id_propietario actual de un coche (puede ser NULL)
     * @param con Conexión activa
     * @param matricula Matrícula del coche
     * @return id_propietario o null si el coche no tiene propietario
     */
    public static Integer obtenerPropietarioActualCoche(Connection con, String matricula) throws SQLException {
        String sql = "SELECT id_propietario FROM coches WHERE matricula = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, matricula);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int idProp = rs.getInt("id_propietario");
                    if (rs.wasNull()) {
                        return null; // El coche no tiene propietario (está en el concesionario)
                    }
                    return idProp;
                }
            }
        }
        return null;
    }

    /**
     * Realiza un traspaso de coche de forma transaccional
     * @param con Conexión activa
     * @param dniComprador DNI del comprador
     * @param matriculaCoche Matrícula del coche
     * @param montoEconomico Precio de la transacción
     * @return true si se realizó correctamente, false si hubo error
     */
    public static boolean realizarTraspaso(Connection con, String dniComprador,
                                           String matriculaCoche, double montoEconomico) {
        try {
            // Desactivar auto-commit para transacción manual
            con.setAutoCommit(false);

            // 1. Verificar que el comprador existe
            int idComprador = obtenerIdPropietario(con, dniComprador);
            if (idComprador == -1) {
                System.err.println("Error: No existe ningún propietario con DNI: " + dniComprador);
                con.rollback();
                con.setAutoCommit(true);
                return false;
            }

            // 2. Verificar que el coche existe
            if (!existeCoche(con, matriculaCoche)) {
                System.err.println("Error: No existe ningún coche con matrícula: " + matriculaCoche);
                con.rollback();
                con.setAutoCommit(true);
                return false;
            }

            // 3. Obtener el propietario actual del coche (puede ser NULL = concesionario)
            Integer idVendedor = obtenerPropietarioActualCoche(con, matriculaCoche);

            // 4. Insertar el traspaso
            String sqlTraspaso = "INSERT INTO traspasos (matricula_coche, id_vendedor, id_comprador, monto_economico) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement pstmtTraspaso = con.prepareStatement(sqlTraspaso);
            pstmtTraspaso.setString(1, matriculaCoche);

            if (idVendedor == null) {
                pstmtTraspaso.setNull(2, java.sql.Types.INTEGER); // Venta del concesionario
            } else {
                pstmtTraspaso.setInt(2, idVendedor);
            }

            pstmtTraspaso.setInt(3, idComprador);
            pstmtTraspaso.setDouble(4, montoEconomico);

            int filasTraspaso = pstmtTraspaso.executeUpdate();
            pstmtTraspaso.close();

            if (filasTraspaso == 0) {
                System.err.println("Error: No se pudo registrar el traspaso");
                con.rollback();
                con.setAutoCommit(true);
                return false;
            }

            // 5. Actualizar el propietario del coche
            String sqlUpdateCoche = "UPDATE coches SET id_propietario = ? WHERE matricula = ?";

            PreparedStatement pstmtUpdate = con.prepareStatement(sqlUpdateCoche);
            pstmtUpdate.setInt(1, idComprador);
            pstmtUpdate.setString(2, matriculaCoche);

            int filasUpdate = pstmtUpdate.executeUpdate();
            pstmtUpdate.close();

            if (filasUpdate == 0) {
                System.err.println("Error: No se pudo actualizar el propietario del coche");
                con.rollback();
                con.setAutoCommit(true);
                return false;
            }

            // 6. Si todo fue bien, hacer COMMIT
            con.commit();
            con.setAutoCommit(true);

            System.out.println("Traspaso realizado correctamente");
            if (idVendedor == null) {
                System.out.println("  - Venta del concesionario al cliente con DNI: " + dniComprador);
            } else {
                System.out.println("  - Venta entre particulares");
            }
            System.out.println("  - Matrícula: " + matriculaCoche);
            System.out.println("  - Monto: " + montoEconomico + "€");

            return true;

        } catch (SQLException e) {
            System.err.println("Error SQL al realizar traspaso: " + e.getMessage());
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            return false;
        }
    }
}