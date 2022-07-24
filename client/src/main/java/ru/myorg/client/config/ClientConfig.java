package ru.myorg.client.config;

import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@ToString
public class ClientConfig {

    private final int port;
    private final String host;
    private final int bufferSize;

    private ClientConfig() {
        try (InputStream in = getClass().getResourceAsStream("/client.properties")) {
            Properties prop = new Properties();
            prop.load(in);

            port = Integer.parseInt(prop.getProperty("port"));
            host = prop.getProperty("host");
            bufferSize = Integer.parseInt(prop.getProperty("bufferSize"));

        } catch (IOException cause) {
            throw new RuntimeException("Error: property file does not exist or unreadable!", cause);
        }
    }

    private static class Holder {
        static ClientConfig instance = new ClientConfig();
    }

    public static ClientConfig getInstance() {
        return Holder.instance;
    }
}
