package util;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {
    private static Properties properties = new Properties();

    // BLOQUE ESTÁTICO -> Se ejecuta AUTOMÁTICAMENTE cuando la clase se usa por primera vez por la (JVM)
    static {

        try {
            //Lo cargamos en memoria
            loadProperties();
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }


    //LEE EL FICHERO .properties
    private static void loadProperties() throws ConfigException{
        // Abrimos el flujo de entrada al archivo config.properties

        try (InputStream input = ConfigProperties.class
                .getClassLoader()
                .getResourceAsStream("config.properties")){

            if(input== null){
                throw new ConfigException("config.properties -> NO encontrado");
            }

            //Cargamos las propiedades del fichero
            properties.load(input);


        } catch (IOException ex) {
            throw new ConfigException("Error al intentar leer el fichero config.properties" , ex);
        }

    }


    // Accedemos a los datos que se cargan en memoria (url, user, pass)
    // -> es como un "getter" del fichero config.properties
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }


}
