package util;

import database.DatabaseManager;
import database.DatabaseInitializer;
import model.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class MenuPrincipal {

    private static final Scanner sc = new Scanner(System.in);

    /**
     * Muestra las opciones del menú en consola.
     */
    private void mostrarOpciones() {
        System.out.println("1)  Conectar BBDD");
        System.out.println("2)  Inicializar/Crear Tablas");
        System.out.println("3)  Registrar Propietario");
        System.out.println("4)  Insertar Coche");
        System.out.println("5)  Importar Coches (CSV)");
        System.out.println("6)  Listar Coches (Concesionario)");
        System.out.println("7)  Listar Coches (Propietarios)");
        System.out.println("8)  Modificar Coche");
        System.out.println("9)  Borrar Coche");
        System.out.println("10) Realizar Traspaso");
        System.out.println("11) Crear Procedimiento");
        System.out.println("12) Ejecutar Procedimiento Almacenado");
        System.out.println("13) Generar Informe Resumen");
        System.out.println("0)  Salir");
        System.out.print("Elija una opción: ");
    }


    public void mostrarMenu() {
        while (true) {
            mostrarOpciones();

            int opcion;
            try {
                opcion = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.println("Entrada inválida. Debe ser un número.");
                continue;
            }

            switch (opcion) {
                case 1 -> opcionConectarBBDD();
                case 2 -> opcionCrearTablas();
                case 3 -> opcionRegistrarPropietario();
                case 4 -> opcionInsertarCoche();
                case 5 -> opcionImportarCochesCSV();
                case 6 -> opcionListarCochesConcesionario();
                case 7 -> opcionListarCochesPropietarios();
                case 8 -> opcionModificarCoche();
                case 9 -> opcionBorrarCoche();
                case 10 -> opcionRealizarTraspaso();
                case 11 -> opcionCrearProcedimiento();
                case 12 -> opcionEjecutarProcedimiento();
                case 13 -> opcionGenerarInforme();

                case 0 -> {
                    System.out.println("\n→ Cerrando conexión a la base de datos...");
                    DatabaseManager.cerrarConexion();
                    System.out.println("Programa finalizado. ¡Hasta pronto!");
                    sc.close();
                    System.exit(0);
                }
                default -> System.err.println("Opción fuera de rango. Intente nuevamente.");
            }
        }
    }

    // ============================================
    // MÉTODOS PRIVADOS (lógica de cada opción)
    // ============================================

    /**
     * Opción 1: Conectar BBDD
     * Permite al usuario elegir entre MySQL o SQLite y establece la conexión.
     * La conexión queda activa para usarse en las demás opciones del menú.
     */
    private void opcionConectarBBDD() {
        System.out.println("1 -> MySQL");
        System.out.println("2 -> SQLite");
        System.out.print("Elija el motor de base de datos: ");

        int opcionMotor;
        try {
            opcionMotor = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.err.println("Entrada inválida. Debe ser 1 o 2.");
            return;
        }

        boolean exito = false;

        try {
            if (opcionMotor == 1) {
                System.out.println("\n→ Conectando a MySQL...");
                exito = DatabaseManager.conectarMySQL();
            } else if (opcionMotor == 2) {
                System.out.println("\n→ Conectando a SQLite...");
                exito = DatabaseManager.conectarSQLite();
            } else {
                System.err.println("Opción no válida. Debe ser 1 (MySQL) o 2 (SQLite).");
                return;
            }

            if (exito) {

                System.out.println("Conexión establecida correctamente");

            } else {

                System.err.println("Error al establecer la conexión");
                System.err.println("Verifique config.properties y que el servidor esté activo");

            }

        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Opción 2: Inicializar/Crear Tablas
     * Lee el archivo DDL apropiado (según el motor conectado) y ejecuta
     * las sentencias CREATE TABLE para las 3 tablas:
     * - propietarios
     * - coches
     * - traspasos
     */
    private void opcionCrearTablas() {




        try {
            // Verificar si hay conexión activa
            if (!DatabaseManager.isConectado()) {
                System.err.println("No hay conexión activa.");
                System.err.println("Primero debe conectar a una base de datos (Opción 1)");
                return;
            }

            // Obtener la conexión activa
            var conexion = DatabaseManager.getConnection();

            // Crear las tablas usando DatabaseInitializer
            System.out.println("Ejecutando scripts DDL...");
            DatabaseInitializer.crearTablas(conexion);

            System.out.println("¡Tablas inicializadas correctamente!");


        } catch (Exception e) {

            System.err.println("Error al crear las tablas:");
            System.err.println("  " + e.getMessage());

        }
    }





    /**
     * Opción 3: Registrar un nuevo propietario
     */
    private void opcionRegistrarPropietario() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== REGISTRAR PROPIETARIO ===");

            System.out.print("DNI: ");
            String dni = sc.nextLine().trim();

            System.out.print("Nombre: ");
            String nombre = sc.nextLine().trim();

            System.out.print("Apellidos: ");
            String apellidos = sc.nextLine().trim();

            System.out.print("Teléfono: ");
            String telefono = sc.nextLine().trim();

            // Obtener conexión y llamar al DAO
            Connection con = DatabaseManager.getConnection();
            boolean exito = PropietarioDAO.registrarPropietario(con, dni, nombre, apellidos, telefono);

            if (exito) {
                System.out.println("✓ Propietario registrado correctamente");
            } else {
                System.err.println("✗ No se pudo registrar el propietario");
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar propietario: " + e.getMessage());
        }
    }

    /**
     * Opción 4: Insertar un nuevo coche al inventario del concesionario
     */
    private void opcionInsertarCoche() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== INSERTAR COCHE ===");

            System.out.print("Matrícula: ");
            String matricula = sc.nextLine().trim();

            System.out.print("Marca: ");
            String marca = sc.nextLine().trim();

            System.out.print("Modelo: ");
            String modelo = sc.nextLine().trim();

            System.out.print("Extras (separados por |): ");
            String extras = sc.nextLine().trim();

            System.out.print("Precio: ");
            double precio = Double.parseDouble(sc.nextLine().trim());

            // Obtener conexión y llamar al DAO
            Connection con = DatabaseManager.getConnection();
            boolean exito = CocheDAO.insertarCoche(con, matricula, marca, modelo, extras, precio);

            if (exito) {
                System.out.println("✓ Coche insertado correctamente");
            } else {
                System.err.println("✗ No se pudo insertar el coche");
            }

        } catch (NumberFormatException e) {
            System.err.println("Error: El precio debe ser un número válido");
        } catch (SQLException e) {
            System.err.println("Error al insertar coche: " + e.getMessage());
        }
    }




    /**
     * Opción 5: Importar coches desde archivo CSV
     */
    private void opcionImportarCochesCSV() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== IMPORTAR COCHES DESDE CSV ===");

            System.out.print("Ruta del archivo CSV: ");
            String rutaCSV = sc.nextLine().trim();

            // Obtener conexión y llamar al DAO
            Connection con = DatabaseManager.getConnection();
            boolean exito = CocheDAO.importarDesdeCsv(con, rutaCSV);

            if (!exito) {
                System.err.println("✗ La importación falló. No se insertó ningún coche (ROLLBACK)");
            }

        } catch (SQLException e) {
            System.err.println("Error al importar CSV: " + e.getMessage());
        }
    }



    /**
     * Opción 6: Listar coches del concesionario (sin propietario)
     */
    private void opcionListarCochesConcesionario() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            Connection con = DatabaseManager.getConnection();
            CocheDAO.listarCochesConcesionario(con);

        } catch (SQLException e) {
            System.err.println("Error al listar coches: " + e.getMessage());
        }
    }



    /**
     * Opción 7: Listar coches que tienen propietario
     */
    private void opcionListarCochesPropietarios() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            Connection con = DatabaseManager.getConnection();
            CocheDAO.listarCochesPropietarios(con);

        } catch (SQLException e) {
            System.err.println("Error al listar coches: " + e.getMessage());
        }
    }



    /**
     * Opción 8: Modificar los datos de un coche existente
     */
    private void opcionModificarCoche() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== MODIFICAR COCHE ===");

            System.out.print("Matrícula del coche a modificar: ");
            String matricula = sc.nextLine().trim();

            Connection con = DatabaseManager.getConnection();

            // Mostrar datos actuales del coche
            if (!CocheDAO.mostrarCoche(con, matricula)) {
                return; // Si no existe, salir
            }

            // Pedir nuevos datos
            System.out.println("Introduce los nuevos datos:");

            System.out.print("Nueva Marca: ");
            String marca = sc.nextLine().trim();

            System.out.print("Nuevo Modelo: ");
            String modelo = sc.nextLine().trim();

            System.out.print("Nuevos Extras (separados por |): ");
            String extras = sc.nextLine().trim();

            System.out.print("Nuevo Precio: ");
            double precio = Double.parseDouble(sc.nextLine().trim());

            // Confirmar modificación
            System.out.print("\n¿Confirmar modificación? (S/N): ");
            String confirmacion = sc.nextLine().trim().toUpperCase();

            if (!confirmacion.equals("S")) {
                System.out.println("Modificación cancelada");
                return;
            }

            // Ejecutar modificación
            boolean exito = CocheDAO.modificarCoche(con, matricula, marca, modelo, extras, precio);

            if (exito) {
                System.out.println("✓ Coche modificado correctamente");
            } else {
                System.err.println("✗ No se pudo modificar el coche");
            }

        } catch (NumberFormatException e) {
            System.err.println("Error: El precio debe ser un número válido");
        } catch (SQLException e) {
            System.err.println("Error al modificar coche: " + e.getMessage());
        }
    }



    /**
     * Opción 9: Borrar un coche de la base de datos
     */
    private void opcionBorrarCoche() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== BORRAR COCHE ===");

            System.out.print("Matrícula del coche a borrar: ");
            String matricula = sc.nextLine().trim();

            Connection con = DatabaseManager.getConnection();

            // Mostrar datos del coche antes de borrar
            if (!CocheDAO.mostrarCoche(con, matricula)) {
                return; // Si no existe, salir
            }

            // Confirmar eliminación
            System.out.print("\n⚠️  ¿Está seguro de que desea eliminar este coche? (S/N): ");
            String confirmacion = sc.nextLine().trim().toUpperCase();

            if (!confirmacion.equals("S")) {
                System.out.println("Operación cancelada");
                return;
            }

            // Ejecutar eliminación
            boolean exito = CocheDAO.borrarCoche(con, matricula);

            if (exito) {
                System.out.println("✓ Coche eliminado correctamente");
            } else {
                System.err.println("✗ No se pudo eliminar el coche");
            }

        } catch (SQLException e) {
            System.err.println("Error al borrar coche: " + e.getMessage());
        }
    }



    /**
     * Opción 10: Realizar un traspaso (venta de coche)
     */
    private void opcionRealizarTraspaso() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== REALIZAR TRASPASO ===");

            System.out.print("DNI del comprador: ");
            String dniComprador = sc.nextLine().trim();

            System.out.print("Matrícula del coche: ");
            String matriculaCoche = sc.nextLine().trim();

            System.out.print("Monto económico de la transacción: ");
            double montoEconomico = Double.parseDouble(sc.nextLine().trim());

            // Confirmar operación
            System.out.print("\n¿Confirmar traspaso? (S/N): ");
            String confirmacion = sc.nextLine().trim().toUpperCase();

            if (!confirmacion.equals("S")) {
                System.out.println("Traspaso cancelado");
                return;
            }

            // Obtener conexión y realizar traspaso
            Connection con = DatabaseManager.getConnection();
            boolean exito = TraspasoDAO.realizarTraspaso(con, dniComprador, matriculaCoche, montoEconomico);

            if (!exito) {
                System.err.println("✗ El traspaso no se completó (ROLLBACK)");
            }

        } catch (NumberFormatException e) {
            System.err.println("Error: El monto debe ser un número válido");
        } catch (SQLException e) {
            System.err.println("Error al realizar traspaso: " + e.getMessage());
        }
    }





    //(11)


    /**
     * Opción 11: Ejecutar procedimiento almacenado (solo MySQL)
     */
    private void opcionEjecutarProcedimiento() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            Connection con = DatabaseManager.getConnection();
            ProcedimientoDAO.ejecutarCochesPorMarca(con);

        } catch (SQLException e) {
            System.err.println("Error al ejecutar procedimiento: " + e.getMessage());
        }
    }





    /**
     * Opción para crear el procedimiento almacenado (solo MySQL)
     */
    private void opcionCrearProcedimiento() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            Connection conexion = DatabaseManager.getConnection();
            DatabaseInitializer.crearProcedimientoAlmacenado(conexion);

        } catch (SQLException | IOException e) {
            System.err.println("Error al crear procedimiento: " + e.getMessage());
        }
    }



    /**
     * Opción 13: Generar informe resumen en archivo de texto
     */
    private void opcionGenerarInforme() {
        if (!DatabaseManager.isConectado()) {
            System.err.println("No hay conexión activa.");
            System.err.println("Primero debe conectar (Opción 1)");
            return;
        }

        try {
            System.out.println("\n=== GENERAR INFORME RESUMEN ===");

            System.out.print("Ruta del archivo (ejemplo: informe_concesionario.txt): ");
            String rutaArchivo = sc.nextLine().trim();

            // Si está vacío, usar nombre por defecto
            if (rutaArchivo.isEmpty()) {
                rutaArchivo = "informe_concesionario.txt";
                System.out.println("Usando nombre por defecto: " + rutaArchivo);
            }

            Connection con = DatabaseManager.getConnection();
            boolean exito = InformeDAO.generarInformeResumen(con, rutaArchivo);

            if (!exito) {
                System.err.println("✗ No se pudo generar el informe");
            }

        } catch (SQLException e) {
            System.err.println("Error al generar informe: " + e.getMessage());
        }
    }
}





