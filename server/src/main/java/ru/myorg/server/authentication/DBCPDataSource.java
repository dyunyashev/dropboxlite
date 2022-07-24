package ru.myorg.server.authentication;

import org.apache.commons.dbcp2.BasicDataSource;
import ru.myorg.server.config.ServerConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class DBCPDataSource {
    private static final DBCPDataSource instance = new DBCPDataSource();
    private final ServerConfig config = ServerConfig.getInstance();
    private final BasicDataSource ds = new BasicDataSource();

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public void putConnection(Connection connection) throws SQLException {
        connection.close();
    }

    public void close() throws SQLException {
        ds.close();
    }

    private DBCPDataSource(){
        ds.setUrl(config.getSql_url());
        ds.setUsername(config.getSql_username());
        ds.setPassword(config.getSql_password());
        ds.setMinIdle(5);
        ds.setMaxIdle(10);
        ds.setMaxOpenPreparedStatements(100);
    }

    public static DBCPDataSource getInstance() {
        return instance;
    }
}
