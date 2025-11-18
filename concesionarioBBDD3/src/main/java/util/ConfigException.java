package util;

/**
 * EXCEPTION - PERSONALIZADA
 * Esta clase se utiliza para representar cuando existe un problema en la carga,
 * lectura o validación de los parámetros de configuración.
 */
public class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        // Llama al constructor de Exception pasando el mensaje y la causa original
        super(message, cause);
    }
}


