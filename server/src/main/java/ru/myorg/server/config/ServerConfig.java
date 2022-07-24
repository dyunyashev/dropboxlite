package ru.myorg.server.config;

import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@ToString
public class ServerConfig {

    private final int port;
    private final int so_backlog;
    private final String sql_url;
    private final String sql_username;
    private final String sql_password;
    private final String mongodb_url;

    private ServerConfig() {
        try (InputStream in = getClass().getResourceAsStream("/server.properties")) {
            Properties prop = new Properties();
            prop.load(in);

            port = Integer.parseInt(prop.getProperty("port"));
            so_backlog = Integer.parseInt(prop.getProperty("so_backlog"));
            sql_url = prop.getProperty("sql_url");
            sql_username = prop.getProperty("sql_username");
            sql_password = prop.getProperty("sql_password");
            mongodb_url = prop.getProperty("mongodb_url");

        } catch (IOException cause) {
            throw new RuntimeException("Error: property file does not exist or unreadable!", cause);
        }
    }

    private static class Holder {
        static ServerConfig instance = new ServerConfig();
    }

    public static ServerConfig getInstance() {
        return Holder.instance;
    }
}
